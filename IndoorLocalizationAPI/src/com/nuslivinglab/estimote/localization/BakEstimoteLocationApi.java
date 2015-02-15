package com.nuslivinglab.estimote.localization;

/**
 * This is a back for previous implementation which 
 * involves some matrix calculation
 * **/

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

public class BakEstimoteLocationApi extends HttpServlet{
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
	public BakEstimoteLocationApi() {
		super();
	}
	
	// methods
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		List<ReceivedBeacon> receivedList = new Vector<ReceivedBeacon>();
		System.out.println("Request is: " + request.toString());
		String[] uuidArray = request.getParameterValues("uuid");
		String[] macAddressArray = request.getParameterValues("mac");
		String[] majorStringArray = request.getParameterValues("major");
		String[] minorStringArray = request.getParameterValues("minor");
		String[] rssiStringArray = request.getParameterValues("rssi");
		String userId = request.getParameter("userid");
		String output = request.getParameter("output");
		
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
		UserLocation result = computeLocation(receivedArray);
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
	
	public UserLocation computeLocation(ReceivedBeacon[] receivedArray) {
		// get the nearby estimote information
		List<EstimoteBeacon> estimoteBeaconList = retrieveBeacon(receivedArray);
		
		// calculate the coordinates
		int numRows = estimoteBeaconList.size();
		
		DenseMatrix64F denseMatrix = new DenseMatrix64F(numRows, 1);
		
		// initiate matrices
		for(int i=0; i<numRows; i++) {
			// matrix a
			EstimoteBeacon ebTemp = estimoteBeaconList.get(i);
			ReceivedBeacon rbTemp = receivedArray[i];
			int value = Math.abs(P0 -rbTemp.getRssi()) + ALPHA;
			denseMatrix.set(i, 0, value);
		}
		
		SimpleMatrix matrix = new SimpleMatrix(denseMatrix);
		matrix.print();
		
		// calculate the estimated location
		SimpleMatrix reverseMatrix = matrix.invert();
		reverseMatrix.print();		

		// check if distance below one meter
		for(int i=0; i<numRows; i++) {
			if(receivedArray[i].getRssi() <= P0) {
				reverseMatrix.set(0, i, W0);
			}
		}
		
		// normalize the matrix
		double sum = reverseMatrix.elementSum();
		reverseMatrix = reverseMatrix.divide(sum);
		
		double estimatedX = 0;
		double estimatedY = 0;
		
		for(int i=0; i<numRows; i++) {
			estimatedX += estimoteBeaconList.get(i).getX() * reverseMatrix.get(0, i);
			estimatedY += estimoteBeaconList.get(i).getY() * reverseMatrix.get(0, i);
		}
		
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
	}
	
	private List<EstimoteBeacon> retrieveBeacon(ReceivedBeacon[] receivedArray) {
		int size = receivedArray.length;
		int number = (size >= NO_BEACON? NO_BEACON : size);
		
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
	
	
	// compute location based on one estimote
//	private String locationByOne() {
//		return null;
//	}
	
	// initiate reference list
//	private void initiateReferenceList() {
//		referenceList = new Hashtable<String, ReferencePoint>();
////		referenceList.put("F8:3E:B0:26:16:BE", new ReferencePoint("F8:3E:B0:26:16:BE", 11.0, 5.8));
////		referenceList.put("D5:7C:6E:F3:BA:8E", new ReferencePoint("D5:7C:6E:F3:BA:8E", 17.5, 5.5));
////		referenceList.put("D8:BB:9F:C6:1E:35", new ReferencePoint("D8:BB:9F:C6:1E:35", 15.0, 2.0));
////		referenceList.put("C7:6D:4B:06:AE:60", new ReferencePoint("C7:6D:4B:06:AE:60", 8.0, 1.0));
////		referenceList.put("C2:21:4E:F6:FE:4F", new ReferencePoint("C2:21:4E:F6:FE:4F", 1.0, 6.0));
////		referenceList.put("C3:B5:05:B6:50:A9", new ReferencePoint("C3:B5:05:B6:50:A9", 5.0, 10.0));
////		referenceList.put("F9:E0:68:95:98:A7", new ReferencePoint("F9:E0:68:95:98:A7", 13.0, 10.0));
////		referenceList.put("DC:1C:20:2D:0E:C1", new ReferencePoint("DC:1C:20:2D:0E:C1", 17.0, 10.0));
//		referenceList.put("F8:3E:B0:26:16:BE", new ReferencePoint("F8:3E:B0:26:16:BE", 11.0, 5.8)); // 0
//		referenceList.put("D5:7C:6E:F3:BA:8E", new ReferencePoint("D5:7C:6E:F3:BA:8E", 17.5, 5.5));
//		referenceList.put("D8:BB:9F:C6:1E:35", new ReferencePoint("D8:BB:9F:C6:1E:35", 15.0, 2.0));
//		referenceList.put("C7:6D:4B:06:AE:60", new ReferencePoint("C7:6D:4B:06:AE:60", 8.0, 1.0));
//		referenceList.put("C2:21:4E:F6:FE:4F", new ReferencePoint("C2:21:4E:F6:FE:4F", 0.5, 1.0));
//		referenceList.put("C3:B5:05:B6:50:A9", new ReferencePoint("C3:B5:05:B6:50:A9", 0.6, 6.5)); //5
//		referenceList.put("F9:E0:68:95:98:A7", new ReferencePoint("F9:E0:68:95:98:A7", 1.0, 10.0));
//		referenceList.put("DC:1C:20:2D:0E:C1", new ReferencePoint("DC:1C:20:2D:0E:C1", 8.9, 10.0));
//		referenceList.put("PB:35:86:65:E8:4E", new ReferencePoint("PB:35:86:65:E8:4E", 8.9, 10.0));
//		referenceList.put("E5:9B:CF:69:85:41", new ReferencePoint("E5:9B:CF:69:85:41", 8.9, 10.0));
//		referenceList.put("CA:08:E7:8E:EF:85", new ReferencePoint("CA:08:E7:8E:EF:85", 8.9, 10.0)); // 10
//		referenceList.put("BC:6A:29:36:BC:81", new ReferencePoint("BC:6A:29:36:BC:81", 8.9, 10.0));
//		
//		
//		
//	}
	
	// test main
	public static void main(String[] args) {
//		String rquestString = "id_0=F8:3E:B0:26:16:BE&"
//				+ "rss_0=-89&"
//				+ "id_1=C7:6D:4B:06:AE:60&"
//				+ "rss_1=-90&id_2=F9:E0:68:95:98:A7&"
//				+ "rss_2=-91&id_3=E5:9B:CF:69:85:41&"
//				+ "rss_3=-93&id_4=D5:7C:6E:F3:BA:8E&"
//				+ "rss_4=-80&id_5=DC:1C:20:2D:0E:C1&"
//				+ "rss_5=-92&id_6=D8:BB:9F:C6:1E:35&"
//				+ "rss_6=-88&id_7=D5:63:BC:2C:74:49&"
//				+ "rss_7=-92";//qinfeng
//		String[] array = rquestString.split("&");
		
//		List<ReceivedSignal> signalList = new Vector<ReceivedSignal>();
//		signalList.add(new ReceivedSignal("F8:3E:B0:26:16:BE", -89));
//		signalList.add(new ReceivedSignal("C7:6D:4B:06:AE:60", -90));
//		signalList.add(new ReceivedSignal("F9:E0:68:95:98:A7", -91));
//		signalList.add(new ReceivedSignal("DC:1C:20:2D:0E:C1", -92));
//		signalList.add(new ReceivedSignal("D8:BB:9F:C6:1E:35", -88));
//		signalList.add(new ReceivedSignal("D5:7C:6E:F3:BA:8E", -80));
		BakEstimoteLocationApi api = new BakEstimoteLocationApi();
		UserLocation result = api.computeLocation(null);
		System.out.println("Result is: " + result.getX());
	}
	
}
