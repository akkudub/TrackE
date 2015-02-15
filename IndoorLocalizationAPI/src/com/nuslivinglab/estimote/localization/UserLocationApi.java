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
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import com.google.gson.Gson;
import com.nuslivinglab.utils.HibernateUtil;

public class UserLocationApi extends HttpServlet {
	// data
	/**
	 * generated serial id
	 */
	private static final long serialVersionUID = -3363123356089704066L;
	private static final String INDIVIDUAL = "individual";
	private static final String RANGE = "range";

	// constructor
	public UserLocationApi() {
		super();
	}

	// methods
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// read parameters
		String type = request.getParameter("type");

		// process request
		List<UserLocation> result;
		if (type.equals(INDIVIDUAL)) {
			String userId = request.getParameter("user_id");
			result = searchIndividual(userId);
		} else if (type.equals(RANGE)) {
			String userId = request.getParameter("user_id");
			Double range = Double.parseDouble(request.getParameter("range"));
			result = searchRange(userId, range);
		} else {
			result = searchAll();
		}

		// out print the result
		String jsonResult = new Gson().toJson(result);

		// print out
		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		out.print(jsonResult);

	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private List<UserLocation> searchIndividual(String userId) {
		List<UserLocation> locationList = null;
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		// try {
		Criteria criteria = session.createCriteria(UserLocation.class);
		criteria.add(Restrictions.eq("userId", userId));
		locationList = criteria.list();

		// close
		tx.commit();

		// } catch(HibernateException e) {
		// if(tx != null) {
		// tx.rollback();
		// }
		// e.printStackTrace();
		// }

		return locationList;
	}

	private List<UserLocation> searchAll() {
		List<UserLocation> locationList = null;
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();
		Criteria criteria = session.createCriteria(UserLocation.class);
		locationList = criteria.list();
		tx.commit();
		return locationList;
	}

	private List<UserLocation> searchRange(String userId, double range) {
		return null;
	}

	public void insertUserRecord(UserLocation record) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		Transaction tx = session.beginTransaction();

		session.saveOrUpdate(record);

		tx.commit();
	}

	// test main
	public static void main(String[] args) {
		UserLocation record = new UserLocation("ivan", 5.0, 5.0, 2.0, System.currentTimeMillis()/1000 );
//		new UserLocationApi().insertUserRecord(record);
		new UserLocationApi().searchAll();
	}
}
