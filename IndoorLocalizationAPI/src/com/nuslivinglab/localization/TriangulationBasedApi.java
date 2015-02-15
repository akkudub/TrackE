package com.nuslivinglab.localization;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TriangulationBasedApi extends HttpServlet{
	// data
	private static Map<String, ReferencePoint> referenceList = null;
	// constructor
	public TriangulationBasedApi() {
		super();
	}
	
	// methods
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		List<ReceivedSignal> receivedList = new Vector<ReceivedSignal>();
		System.out.println("Request is: " + request.toString());
		boolean hasMore = true;
		String maxId = request.getParameter("id_0");
		Double maxRss = Double.parseDouble(request.getParameter("rss_0"));
		ReceivedSignal maxSignal = new ReceivedSignal(maxId, maxRss);
		int i = 1;
		while (hasMore) {
			String receivedId = request.getParameter("id_" + i);
			if(receivedId != null) {
				String value = request.getParameter("rss_" + i);
				Double receivedRss = Double.parseDouble(value);
				ReceivedSignal receivedSignal = new ReceivedSignal(receivedId, receivedRss);
				if (maxSignal.getRss() < receivedSignal.getRss()) {
					receivedList.add(maxSignal);
					maxSignal = receivedSignal;
				} else {
					receivedList.add(receivedSignal);
				}
			} else {
				hasMore = false;
			}
			i++;
		}
		// add in the max one to the end of list
		receivedList.add(maxSignal);
		System.out.println("Number of beacons: " + receivedList.size());
		
		// sort the list
		ReceivedSignal[] receivedArray = new ReceivedSignal[receivedList.size()];
		for(int j=0; j<receivedList.size(); j++ ) {
			receivedArray[j] = receivedList.get(j);
		}
		
		for(int ii=0; ii < 3; ii++) {
			ReceivedSignal maxOne = receivedArray[0];
			int maxIndex = 0;
			for(int jj=0; jj <= receivedList.size() - 2 - ii; jj++) {
				ReceivedSignal temp = receivedArray[jj];
				if(maxOne.getRss() < temp.getRss()) {
					maxOne = temp;
					maxIndex = jj;
				}
			}
			// swap the max one with last one
			receivedArray[maxIndex] = receivedArray[receivedList.size() - 2 - ii] ;
			receivedArray[receivedList.size() - 2 - ii] = maxOne;
		}
		
		List<ReceivedSignal> sortedReceivedList = new Vector<ReceivedSignal>();
		for(int kk=3; kk>=0; kk--) {
			int length = receivedArray.length;
			sortedReceivedList.add(receivedArray[length-1-kk]);
		}
		
		// get the result
//		String result = computeLocation(receivedList);
		String result = computeLocation(sortedReceivedList);
//		result = "<response>" + result + "</response>";
//		String result = "test result";
		
		// if json result is required
		String[] resultArray = result.split(";");
		JsonArray jArray = new JsonArray();
		Gson gson = new GsonBuilder().serializeNulls().create();
		JsonObject obj = new JsonObject();
		
		obj.addProperty("x",resultArray[0].split("=")[1]);
		obj.addProperty("y",resultArray[1].split("=")[1]);
		
		jArray.add(obj);
		String jsonResult = gson.toJson(jArray);

		// output
		try {
			PrintWriter out = response.getWriter();
			response.setContentType("text/plain");
			out.println(result);
//			resp.setContentType("application/json");
//			out.println(jsonResult);
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
	
	public String computeLocation(List<ReceivedSignal> receivedList) {
		if(referenceList == null) {
			initiateReferenceList();
		}
		String resultString = "";
		ReceivedSignal largeSignal = receivedList.get(receivedList.size()-1);
//		String rPoint = rPoint;
//		double referencedRss = receivedList.get(referencedId);
		ReferencePoint rPoint = referenceList.get(largeSignal.getId());
		System.out.println("R_X: " + rPoint.getX());
		System.out.println("R_Y: " + rPoint.getY());
		double dr = rPoint.computeDistance(largeSignal.getRss());
		double kr = rPoint.getK();
		
		resultString += "Selected R Point: " + largeSignal.getId() + "\n" 
				+ "Received RSS: " + largeSignal.getRss() + "\n"
				+ "Distance: " + dr + "\n";
		
		int numRows = receivedList.size()-1;
		
		DenseMatrix64F denseMatrixA = new DenseMatrix64F(numRows, 2);
		DenseMatrix64F denseMatrixP = new DenseMatrix64F(numRows, 1);
		
		// initiate matrices
		for(int i=0; i<numRows; i++) {
			ReceivedSignal normalSignal = receivedList.get(i);
			ReferencePoint normalPoint = referenceList.get(normalSignal.getId());
			// matrix a
			denseMatrixA.set(i, 0, normalPoint.getX() - rPoint.getX());
			denseMatrixA.set(i, 1, normalPoint.getY() - rPoint.getY());
			// matrix p
			Double di = normalPoint.computeDistance(normalSignal.getRss());
			double ki = normalPoint.getK();
			Double pValue = dr*dr - di*di - (kr-ki); 
			denseMatrixP.set(i, 0, pValue);
			
			// debug purpose return
			resultString += "Point: " + normalSignal.getId() + "\n" 
					+ "Received RSS: " + normalSignal.getRss() + "\n"
					+ "Distance: " + di + "\n";
			
//			return
		}
		
		SimpleMatrix matrixA = new SimpleMatrix(denseMatrixA);
		SimpleMatrix matrixP = new SimpleMatrix(denseMatrixP);
		matrixA.print();
		matrixP.print();
		
		// calculate the estimated location
		SimpleMatrix result = (matrixA.transpose().mult(matrixA)).invert()
				.mult(matrixA.transpose())
				.mult(matrixP);
		result.print();		
		double estimatedX = result.get(0, 0)*0.5;
		double estimatedY = result.get(1, 0)*0.5;
		
		

		
	
		
		return resultString + "x=" + estimatedX + ";y=" + estimatedY;
	}
	
	// initiate reference list
	private void initiateReferenceList() {
		referenceList = new Hashtable<String, ReferencePoint>();
//		referenceList.put("F8:3E:B0:26:16:BE", new ReferencePoint("F8:3E:B0:26:16:BE", 11.0, 5.8));
//		referenceList.put("D5:7C:6E:F3:BA:8E", new ReferencePoint("D5:7C:6E:F3:BA:8E", 17.5, 5.5));
//		referenceList.put("D8:BB:9F:C6:1E:35", new ReferencePoint("D8:BB:9F:C6:1E:35", 15.0, 2.0));
//		referenceList.put("C7:6D:4B:06:AE:60", new ReferencePoint("C7:6D:4B:06:AE:60", 8.0, 1.0));
//		referenceList.put("C2:21:4E:F6:FE:4F", new ReferencePoint("C2:21:4E:F6:FE:4F", 1.0, 6.0));
//		referenceList.put("C3:B5:05:B6:50:A9", new ReferencePoint("C3:B5:05:B6:50:A9", 5.0, 10.0));
//		referenceList.put("F9:E0:68:95:98:A7", new ReferencePoint("F9:E0:68:95:98:A7", 13.0, 10.0));
//		referenceList.put("DC:1C:20:2D:0E:C1", new ReferencePoint("DC:1C:20:2D:0E:C1", 17.0, 10.0));
		referenceList.put("F8:3E:B0:26:16:BE", new ReferencePoint("F8:3E:B0:26:16:BE", 11.0, 5.8)); // 0
		referenceList.put("D5:7C:6E:F3:BA:8E", new ReferencePoint("D5:7C:6E:F3:BA:8E", 17.5, 5.5));
		referenceList.put("D8:BB:9F:C6:1E:35", new ReferencePoint("D8:BB:9F:C6:1E:35", 15.0, 2.0));
		referenceList.put("C7:6D:4B:06:AE:60", new ReferencePoint("C7:6D:4B:06:AE:60", 8.0, 1.0));
		referenceList.put("C2:21:4E:F6:FE:4F", new ReferencePoint("C2:21:4E:F6:FE:4F", 0.5, 1.0));
		referenceList.put("C3:B5:05:B6:50:A9", new ReferencePoint("C3:B5:05:B6:50:A9", 0.6, 6.5)); //5
		referenceList.put("F9:E0:68:95:98:A7", new ReferencePoint("F9:E0:68:95:98:A7", 1.0, 10.0));
		referenceList.put("DC:1C:20:2D:0E:C1", new ReferencePoint("DC:1C:20:2D:0E:C1", 8.9, 10.0));
		referenceList.put("PB:35:86:65:E8:4E", new ReferencePoint("PB:35:86:65:E8:4E", 8.9, 10.0));
		referenceList.put("E5:9B:CF:69:85:41", new ReferencePoint("E5:9B:CF:69:85:41", 8.9, 10.0));
		referenceList.put("CA:08:E7:8E:EF:85", new ReferencePoint("CA:08:E7:8E:EF:85", 8.9, 10.0)); // 10
		referenceList.put("BC:6A:29:36:BC:81", new ReferencePoint("BC:6A:29:36:BC:81", 8.9, 10.0));
		
		
		
	}
	
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
		
		List<ReceivedSignal> signalList = new Vector<ReceivedSignal>();
		signalList.add(new ReceivedSignal("F8:3E:B0:26:16:BE", -89));
		signalList.add(new ReceivedSignal("C7:6D:4B:06:AE:60", -90));
		signalList.add(new ReceivedSignal("F9:E0:68:95:98:A7", -91));
		signalList.add(new ReceivedSignal("DC:1C:20:2D:0E:C1", -92));
		signalList.add(new ReceivedSignal("D8:BB:9F:C6:1E:35", -88));
		signalList.add(new ReceivedSignal("D5:7C:6E:F3:BA:8E", -80));
		TriangulationBasedApi api = new TriangulationBasedApi();
		String result = api.computeLocation(signalList);
		System.out.println("Result is: " + result);
	}
	
}
