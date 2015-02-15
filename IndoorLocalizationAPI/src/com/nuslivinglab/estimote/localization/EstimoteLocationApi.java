package com.nuslivinglab.estimote.localization;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuslivinglab.utils.HibernateUtil;

public class EstimoteLocationApi extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3775950932392824009L;
	// data
//	private static Map<String, ReferencePoint> referenceList = null;
	private static final int P0 = -60;
	private static final int ALPHA = 0;
	private static final int W0 = 2; // weighting factor below 1 meter
	private static final double ACCURACY = 2.0; // accuracy
	private static final int NO_BEACON = 4;
	
	// constructor
	public EstimoteLocationApi() {
		super();
	}
	
	// methods
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		List<ReceivedBeacon> receivedList = new Vector<ReceivedBeacon>();
//		System.out.println("Request is: " + request.toString());
		String[] uuidArray = request.getParameterValues("uuid");
		String[] macAddressArray = request.getParameterValues("mac");
		String[] majorStringArray = request.getParameterValues("major");
		String[] minorStringArray = request.getParameterValues("minor");
		String[] rssiStringArray = request.getParameterValues("rssi");
		String userId = request.getParameter("userid");
		String output = request.getParameter("output");
		String method = request.getParameter("method");
		
		ReceivedBeacon[] receivedArray = new ReceivedBeacon[uuidArray.length];
		
		for(int i=0; i<uuidArray.length; i++) {
			String uuidTemp = uuidArray[i];
			String macAddressTemp = macAddressArray[i];
			int majorTemp = Integer.parseInt(majorStringArray[i]);
			int minorTemp = Integer.parseInt(minorStringArray[i]);
			int rssiTemp = Integer.parseInt(rssiStringArray[i]);
			ReceivedBeacon receivedBeaconTemp = new ReceivedBeacon(uuidTemp, macAddressTemp, 
					majorTemp, minorTemp, rssiTemp);
			receivedArray[i] = receivedBeaconTemp;
		}

		// sort the list
		sortBeacon(receivedArray);
		
		// get the result
//		String result = computeLocation(receivedList);
		UserLocation result = computeLocation(receivedArray, method);
//		result = "<response>" + result + "</response>";
//		String result = "test result";
		
		// update with users_location talbe
		result.setUserId(userId);
		updateUserLocation(result);
		
		// if json result is required
//		String[] resultArray = result.split(";");
		JsonArray jArray = new JsonArray();
		Gson gson = new GsonBuilder().serializeNulls().create();
		JsonObject obj = new JsonObject();
		
		obj.addProperty("x",result.getX());
		obj.addProperty("y",result.getY());
		obj.addProperty("accuracy", result.getAccuray());
		
		jArray.add(obj);
		String jsonResult = gson.toJson(jArray);

		// output
		try {
			PrintWriter out = response.getWriter();
//			response.setContentType("text/plain");
//			out.println(result);
			response.setContentType("application/json");
			out.println(jsonResult);
		} catch (IOException e) {
			
//			return
			e.printStackTrace();
		}
		
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		doGet(request, response);
	}
	
	public UserLocation computeLocation(ReceivedBeacon[] receivedArray, String method) {
		// get the nearby estimote information
		List<EstimoteBeacon> estimoteBeaconList = retrieveBeacon(receivedArray, method);
		
		// test result
//		for(EstimoteBeacon eb : estimoteBeaconList) {
//			System.out.println(eb.toString());
//		}
		
		// calculate the coordinates
		int numRows = estimoteBeaconList.size();
		
//		DenseMatrix64F denseMatrix = new DenseMatrix64F(numRows, 1);
		double[] weightArray = new double[numRows];
		double sum = 0;
		
		double estimatedX = 0;
		double estimatedY = 0;
		// initiate matrices
		for(int i=0; i<numRows; i++) {
			// matrix a
			EstimoteBeacon ebTemp = estimoteBeaconList.get(i);
			ReceivedBeacon rbTemp = receivedArray[i];
			if(rbTemp.getRssi() <= P0) { // check if distance below one meter
				weightArray[i] = W0;
			} else {
				int value = Math.abs(P0 -rbTemp.getRssi()) + ALPHA;
				weightArray[i] = (double) 1 / value; // calculate the weight
			}
			sum += weightArray[i];
			estimatedX += estimoteBeaconList.get(i).getX() * weightArray[i];
			estimatedY += estimoteBeaconList.get(i).getY() * weightArray[i];
		}
		
		// normalize
		estimatedX = estimatedX / sum;
		estimatedY = estimatedY / sum;
		
		Long timestamp = System.currentTimeMillis()/1000;
		
		return new UserLocation(null, estimatedX, estimatedY, ACCURACY, timestamp);
//		return resultString + "x=" + estimatedX + ";y=" + estimatedY;
	}
	
	private void sortBeacon(ReceivedBeacon[] receivedArray) {
		int length = receivedArray.length;
		for(int i=0; i<length; i++) {
			// find the largest one in the remaining array elements
			int max = i;
			for(int j=i; j<length; j++) {
				if(receivedArray[j].compareTo(receivedArray[max]) > 0) {
					max = j;
				}
			}
			// swap elements if i is not the largest one
			if(max != i) {
				ReceivedBeacon rbTemp = receivedArray[i];
				receivedArray[i] = receivedArray[max];
				receivedArray[max] = rbTemp;
			}
			
		}
		
		// print out result
//		System.out.println("Sorted Result: ");
//		for(int i=0; i<length; i++) {
//			System.out.println(receivedArray[i].toString());
//		}
	}
	
	private List<EstimoteBeacon> retrieveBeacon(ReceivedBeacon[] receivedArray, String method) {
		int size = receivedArray.length;
		int number = 0; 
		if(method.equals("one")) {
			number = 1;
		} else if(method.equals("two")) {
			number = 2;
		} else {
			number = 4;
		}
		
		// double check whether the number of received beacons is larger than the wanted number
		if(size < number) {
			number = size;
		}
		
		// retrieve the beacons from database
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		
		Criteria criteria = session.createCriteria(EstimoteBeacon.class);
		
		Criterion[] criterionArray = new Criterion[number];
		
		for(int i=0; i<number; i++) {
			ReceivedBeacon receivedBeacon = receivedArray[i];
			criterionArray[i] =  Restrictions.and(Restrictions.eq("major", receivedBeacon.getMajor()),
					Restrictions.eq("minor", receivedBeacon.getMinor()));
		}
		criteria.add(Restrictions.disjunction(criterionArray));
		
		List<EstimoteBeacon> result = criteria.list();
		
		// close transaction
		tx.commit();
		
		return result;
	}
	
	// update user current location
	private void updateUserLocation(UserLocation userLocation) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		session.saveOrUpdate(userLocation);
		tx.commit();
	}
	
	
	
	// test main
	public static void main(String[] args) {
		ReceivedBeacon r1 = new ReceivedBeacon("","",10001, 10001, -80);
		ReceivedBeacon r2 = new ReceivedBeacon("","",10001, 10002, -80);
		ReceivedBeacon r3 = new ReceivedBeacon("","",10001, 10003, -78);
		ReceivedBeacon r4 = new ReceivedBeacon("","",10001, 10004, -78);
		ReceivedBeacon r5 = new ReceivedBeacon("","",10002, 10001, -76);
		ReceivedBeacon r6 = new ReceivedBeacon("","",10002, 10002, -76);
		ReceivedBeacon r7 = new ReceivedBeacon("","",10002, 10003, -95);
		
		ReceivedBeacon[] array = {r1, r2, r3, r4, r5, r6, r7};
		new EstimoteLocationApi().sortBeacon(array);
//		List<EstimoteBeacon> list = new EstimoteLocationApi().retrieveBeacon(array);
//		System.out.println("Size of list: " + list.size());
//		for(EstimoteBeacon eb : list) {
//			System.out.println(eb.toString());
//		}
		
		UserLocation ul = new EstimoteLocationApi().computeLocation(array, "one");
		System.out.println(ul.toString());
	}
	
}
