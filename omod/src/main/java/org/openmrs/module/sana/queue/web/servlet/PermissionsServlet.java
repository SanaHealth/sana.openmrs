package org.openmrs.module.sana.queue.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.sana.ModuleConstants;
import org.openmrs.module.sana.ModuleConstants.Privilege;
import org.openmrs.module.sana.api.MDSResponse;
import org.openmrs.User;

import com.google.gson.Gson;

/**
 * Handles Requests to verify if a user has permission to interact with Sana 
 * queue
 * 
 * @author Sana Development Team
 *
 */
public class PermissionsServlet extends HttpServlet {

    private static final long serialVersionUID = 4847187771370210197L;
    private Log log = LogFactory.getLog(this.getClass());

    /**
     * Called when a transaction is successful.
     * @param request The originating request
     * @param response The servlet response object
     * @param message The message to return
     */
    private void succeed(HttpServletRequest request, 
    		HttpServletResponse response, String message) 
    {
        try {
        	MDSResponse mdsresponse = new MDSResponse();
        	mdsresponse.status = MDSResponse.SUCCESS;
        	mdsresponse.code = "unspecified";
        	mdsresponse.message = message;
        	Gson g = new Gson();
        	String response_json = g.toJson(mdsresponse);
        	
        	//get the PrintWriter object to write the html page
        	response.setContentType("text/html");
        	PrintWriter out = response.getWriter();
        	out.write(response_json);
        	out.flush();
        	out.close();
        } catch(IOException e){
        	log.error("problem while writing succeed():" + e.toString());
        }
    }

    /**
     * Called when a transaction fails.
     * @param request The originating request
     * @param response The servlet response object
     * @param message The message to return
     */
    private void fail(HttpServletRequest request, HttpServletResponse response,
    		String message) 
    {
        try {
        	MDSResponse mdsresponse = new MDSResponse();
        	mdsresponse.status = MDSResponse.FAILURE;
        	mdsresponse.code = "unspecified";
        	mdsresponse.message = message;
        	Gson g = new Gson();
        	String response_json = g.toJson(mdsresponse);
        	
        	//get the PrintWriter object to write the html page
        	response.setContentType("text/html");
        	PrintWriter out = response.getWriter();
        	out.write(response_json);
        	out.flush();
        	out.close();
        } catch(IOException e){
        	log.error("problem while writing fail(): " + e.toString());
        }
    }

	/**
	 * Check if current user has permissions to manage the queue in order to 
	 * upload a procedure	 
	 */
    @Override
    protected void doGet(HttpServletRequest request, 
    		HttpServletResponse response) throws ServletException, IOException 
    {   
    	try{
    		User currentUser = Context.getAuthenticatedUser();
    		String managePrivilege = Privilege.MANAGE_QUEUE;
    	
    		if(currentUser.hasPrivilege(managePrivilege)){
    			log.info("User " + currentUser.getGivenName() + " " 
    				+ currentUser.getFamilyName() + "does have privilege");
    			succeed(request, response, "User has Manage Sana Queue privileges");
    		} else{
    			log.info("User does NOT have privilege");
    		    fail(request, response, "User does NOT have privilege");
    		}
    	} catch(Exception e){
    		log.error("doGet(): " + e.toString());
    		fail(request, response, e.toString());
    	}
    }
    
    @Override
    protected void doPost(HttpServletRequest request, 
    		HttpServletResponse response) throws ServletException, IOException 
    {  
    	String username = null;
    	String password = null;
    	try{
        	username = request.getParameter("username");
        	password = request.getParameter("password");
    		Context.authenticate(username, password);
    		User user = Context.getAuthenticatedUser();
    		//Context.becomeUser(user.getSystemId());
    		//Context.isAuthenticated();
    		String managePrivilege = Privilege.MANAGE_QUEUE;
    	
    		if(user.hasPrivilege(managePrivilege)){
    			log.info("User " + user.getGivenName() + " " 
    				+ user.getFamilyName() + "does have privilege");
    			succeed(request, response, "User has Manage Sana Queue privileges");
    		} else{
    			log.info("User " + user.getGivenName() + " " 
    				+ user.getFamilyName() + "does NOT have privilege");
    			fail(request, response, "User " + user.getGivenName() + " " 
    				+ user.getFamilyName() 
    				+ "does not have Manage Sana Queue Privileges");
    		}
    	} catch (ContextAuthenticationException e){
    		log.error("Authentication Failure. username: " + username);
    	} catch (Exception e){
    		log.error("Authentication Failure. error: " + e.toString());
    	}
    }
}


