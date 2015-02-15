package com.nuslivinglab.estimote.localization;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.nuslivinglab.utils.HibernateUtil;

public class EstimoteBeaconApi extends HttpServlet {
	// data
	/**
	 * 
	 */
	private static final long serialVersionUID = 221532849614839989L;
	private static final String ALL = "all";
	private static final String INDIVIDUAL = "individual";

	// constructor
	public EstimoteBeaconApi() {
		super();
	}

	// methods
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// read parameters
		String type = request.getParameter("type");

		// compute result
		List<EstimoteBeacon> result;
		if (type.equals(INDIVIDUAL)) {
			String uuid = request.getParameter("uuid");
			String mac = request.getParameter("mac");
			// int major = Integer.parseInt(request.getParameter("major"));
			// int minor = Integer.parseInt(request.getParameter("minor"));
			String majorString = request.getParameter("major");
			String minorString = request.getParameter("minor");
			result = searchIndividual(uuid, mac, majorString, minorString);
		} else {
			result = searchAll();
		}

		// output
		String jsonResult = new Gson().toJson(result);

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		out.print(jsonResult);

	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public List<EstimoteBeacon> searchAll() {
		List<EstimoteBeacon> beaconList = null;
		Session session = null;
		Transaction tx = null;
		try {
			// prepare connection
			session = HibernateUtil.getSessionFactory().getCurrentSession();
			tx = session.beginTransaction();

			Criteria criteria = session.createCriteria(EstimoteBeacon.class);
			beaconList = criteria.list();
//			Query query = session.createSQLQuery("select * from beacons");
//			Query query = session.createQuery("from EstimoteBeacon");
//			List list = query.list();
			
			System.out.println("Beacon List size: " + beaconList.size());
			// close transaction
			tx.commit();

		} catch (HibernateException e) {
//			if (tx != null) {
//				tx.rollback();
//			}
			e.printStackTrace();
		}
		return beaconList;
	}

	public List<EstimoteBeacon> searchIndividual(String uuid, String mac,
			String majorString, String minorString) {
		List<EstimoteBeacon> beaconList = null;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().getCurrentSession();
			tx = session.beginTransaction();
			Criteria criteria = session.createCriteria(EstimoteBeacon.class);
			if (uuid != null) {
				criteria.add(Restrictions.eq("uuid", uuid));
			}

			if (mac != null) {
				criteria.add(Restrictions.eq("mac", mac));
			}

			if (majorString != null) {
				criteria.add(Restrictions.eq("major",
						Integer.parseInt(majorString)));
			}

			if (minorString != null) {
				criteria.add(Restrictions.eq("minor",
						Integer.parseInt(minorString)));
			}

			beaconList = criteria.list();
			System.out.println("Result size: " + beaconList.size());
			
			// close
			tx.commit();

		} catch (HibernateException e) {
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace();
		}
		return beaconList;
	}

	// test main
	public static void main(String[] args) {
		new EstimoteBeaconApi().searchAll();
//		new EstimoteBeaconApi().searchIndividual(
//				"b9407f30-f5f8-466e-aff9-25556b57fe6d", null, null, "10002");
	}

}
