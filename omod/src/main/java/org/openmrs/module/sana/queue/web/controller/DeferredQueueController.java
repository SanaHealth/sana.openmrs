package org.openmrs.module.sana.queue.web.controller;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
import javax.imageio.ImageIO;
import javax.print.DocFlavor.STRING;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.openmrs.module.sana.queue.QueueItemXml;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonName;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sana.ModuleConstants;
import org.openmrs.module.sana.ModuleConstants.Property;
import org.openmrs.module.sana.queue.*;
import org.openmrs.obs.ComplexData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
/**
 * This controller backs the /web/module/queue.jsp page. This controller is tied to that
 * jsp page in the /metadata/moduleApplicationContext.xml file
 */
//@Controller
//@RequestMapping(value = "module/sana/queue/v1/queueDeferred.form")
public class DeferredQueueController{

    protected final Log log = LogFactory.getLog(getClass());

    /** Success form view name */
    private final String HTML_SUCCESS_VIEW = ModuleConstants.FORM_PATH + "queueDeferred";
    private final String JSON_SUCCESS_VIEW =ModuleConstants.FORM_PATH + "jsonformat";
    private final String XML_SUCCESS_VIEW =ModuleConstants.FORM_PATH + "xmlformat";
    
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) {
    	
    	HttpSession httpsession=request.getSession();
    	System.out.println("checkDate:"+request.getParameter("comboDate"));
    	int defcount = Integer.parseInt(Context.getAdministrationService().getGlobalProperty(Property.DISPLAY_COUNT));
    	 String formpage=ModuleConstants.FORM_PATH + "queueDeferred";
    	int queuelistcount = Integer.parseInt(Context.getAdministrationService().getGlobalProperty(Property.MAX_QUEUE_ITEMS));
    	Integer queuelistcountObj = new Integer(queuelistcount);
    	String queueLimit = request.getParameter("queueLimit");
    	int sortvalue=0;
    	String sortstring = request.getParameter("hidsortname");
    	if(sortstring != null && sortstring != "")
    	{
    		sortvalue = Integer.parseInt(sortstring);
    		
    	}
    	else
    	{
    		sortvalue = 1;
    	}	
    	String checkProo = "SHOW ALL";
    	int iArchieveState = 1;
    	

    	
    	int endvalue = queuelistcount;
    	if( request.getParameter("queuelimitname") != null &&  request.getParameter("queuelimitname") != "")
    		endvalue = Integer.parseInt( request.getParameter("queuelimitname"));
    	int startvalue = 0; 
    	
    	System.out.println("gotopagename :"+request.getParameter("gotopagename"));
    	if(request.getParameter("gotopagename") != "" && request.getParameter("gotopagename") != null )
    	{   
    		int substract = Integer.parseInt(request.getParameter("gotopagename"));
    		if( request.getParameter("queuelimitname") != null &&  request.getParameter("queuelimitname") != "")
    		{
        		endvalue = Integer.parseInt( request.getParameter("queuelimitname"));
    		    startvalue = Integer.parseInt(request.getParameter("gotopagename"))*endvalue;
    		    System.out.println("start and end:"+startvalue+":"+endvalue+":"+substract);
    		    startvalue =startvalue-endvalue-(substract-1);
    		    System.out.println("start value1:"+startvalue);
    		}
    		else
    		{
    			
    		    startvalue = Integer.parseInt(request.getParameter("gotopagename"))*queuelistcount;
    		    System.out.println("start and end:"+startvalue+":"+endvalue+":"+substract);
    		    startvalue =startvalue-endvalue-(substract-1);
    			System.out.println("start value2:"+startvalue);
    		}
    	}	
    	else
	    	if( request.getParameter("hidprevname") != null &&  request.getParameter("hidprevname") != "")
	    	{  
	    		System.out.println("in else :");
	    	
	    		startvalue = Integer.parseInt(request.getParameter("hidprevname"));
	    		System.out.println("in else :"+startvalue);
	    	}	
    	System.out.println("startvalue:"+startvalue+"endvalue:"+endvalue);
    	
    	
		QueueItemService queueService = Context.getService(QueueItemService.class);
		
		StringBuffer sbr = null;
		//checking archived button is submited or not.
		if(request.getParameter("subarchivename")!=null) 
    	{
    		
			// chklist is Hidden Text field, contains checked ids from queue (appended all IDs).
			String checkedids = request.getParameter("chklist"); 
	    	if(checkedids != null && checkedids!="")
	    	{
	        	String strcheckedlist = checkedids;
	        	String[] temp;
	        	//splitting the checked ids string into array[] values.
	        	temp =strcheckedlist.split(";");
	        	
	        	int intarr[] = new int[temp.length];
	        	for(int i=0;i<temp.length;i++)
	        	{	
	        		intarr[i]=Integer.parseInt(temp[i]);
	        		
	        		
	        	}
	        	//function for "Archiving" intarr[] values.
	        	queueService.getUnArchivedRows(intarr);
	    	
	    	}
    	}	
		
		
		User strUserName = Context.getAuthenticatedUser();
		String strprocedure = request.getParameter("proname");
        String  checkPro = request.getParameter("comboPro");
        String  checkDate = request.getParameter("comboDate");
        String strArchieve = request.getParameter("optionarchive");
    	if(strArchieve != null)
    	{
    	iArchieveState = 1;
    	if(strArchieve.equalsIgnoreCase("Show ALL"))
    			iArchieveState = 0;
    	else if(strArchieve.equalsIgnoreCase("Show InActive"))
    		iArchieveState = 2;
    	else if(strArchieve.equalsIgnoreCase("Show Active"))
    		iArchieveState = 1;
    	}
    	int strdate = 365;
        
        int strmonth = 0;
        try{
        if(request.getParameter("daysname") != null && request.getParameter("daysname") != "")
        {
        	
        	String strdaysname = request.getParameter("daysname");
        	if(strdaysname != null && strdaysname != ""){
        		strdate=Integer.parseInt(strdaysname);
        		
        	}	
        	System.out.println("days"+strdate+"iArc:"+iArchieveState+"checkDate:"+checkDate);
        }
        if(request.getParameter("daysarcname") != "" && request.getParameter("daysarcname") != null)// && request.getParameter("monthname") != "")
        {       
        	    
	    		String strdaysname=request.getParameter("daysarcname");
	        	if(strdaysname!=null && strdaysname != "")
	        		strdate=Integer.parseInt(strdaysname);
	        	checkDate = null;
	        	iArchieveState = 3;
	        	System.out.println("days"+strdate+"iArc:"+iArchieveState+"checkDate:"+checkDate);
        }
        }catch(Exception e) { e.printStackTrace();}
        int days = strdate;
        //if(days != 365)
        defcount = days;
        String procedure = null;
		if(strprocedure != null && strprocedure != "" )
			procedure = strprocedure;
		if(checkPro != null && checkPro != "")
			 checkProo = checkPro;
		System.out.println("checkPro:"+checkProo);
		System.out.println("Days2:"+days);
		System.out.println("checkDate"+checkDate);
		int totalrows = queueService.getProDateRowsDeferredCount(procedure,defcount,checkProo, checkDate, iArchieveState,startvalue,endvalue,sortvalue);
    	List<QueueItem> items = queueService.getProDateRowsDeferred(procedure,defcount,checkProo, checkDate, iArchieveState,startvalue,endvalue,sortvalue);
    	Map map=new HashMap();
    	List<QueueItem> procedurelist = queueService.getProcedureAllRows();
    	List<DateItems> dateitems = queueService.getDateMonths();
    	map.put("queueItems", items);
    	map.put("procedurerows",procedurelist);
    	map.put("dateItems", dateitems);
    	map.put("queuelistcount",queuelistcountObj);
    	StringBuffer data=new StringBuffer();
    	data.append("<");
    	data.append("xml");
    	data.append(">");
    	map.put("queuesize",items.size());
    	map.put("data",data);
    	request.setAttribute("proname", "SHOW ALL");
        log.info("Returning " + items.size() + "axx deferred queue items" );
        
        if(request.getParameter("disformate") == null || request.getParameter("disformate").equalsIgnoreCase("htmlformat"))
        {
        	System.out.println("formate:"+request.getParameter("disformate"));
            return new ModelAndView(HTML_SUCCESS_VIEW, "map",map);
        }
        else
        
            if(request.getParameter("disformate").equalsIgnoreCase("Jsonformat"))
            {
            QueueItemJson qij = new QueueItemJson();
            sbr = qij.encode(items);
            
            Map map1=new HashMap();
            map1.put("sbr",sbr.toString());
            return new ModelAndView(JSON_SUCCESS_VIEW, "sbr", map1);
            }
       
        else
        	if(request.getParameter("disformate").equalsIgnoreCase("Xmlformat"))
        	{
        		QueueItemXml qix=new QueueItemXml();
        		
	            sbr = qix.encode(items);
	            PatientResource pr = new PatientResource();
	            try{
	            	
	            
	            PrintWriter pw=response.getWriter();
	            pr.printPatientList(pw, sbr);
	            }
	            catch(Exception e)
	            {
	            	e.printStackTrace();
	            }
	           
	            Map map1=new HashMap();
	           
	            map1.put("sbr",sbr);
	            return new ModelAndView(XML_SUCCESS_VIEW, "sbr", map1);
        	}
        
        return null;
	}
}