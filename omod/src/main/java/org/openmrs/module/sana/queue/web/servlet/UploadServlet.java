package org.openmrs.module.sana.queue.web.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptWord;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientIdentifierException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sana.ModuleConstants;
import org.openmrs.module.sana.ModuleConstants.Property;
import org.openmrs.module.sana.api.MDSMessage;
import org.openmrs.module.sana.api.MDSQuestion;
import org.openmrs.module.sana.api.MDSResponse;
import org.openmrs.module.sana.queue.QueueItem;
import org.openmrs.module.sana.queue.QueueItemService;
import org.openmrs.module.sana.queue.QueueItemStatus;
import org.openmrs.obs.ComplexData;

import com.google.gson.Gson;

/**
 * Provides encounter upload services to the Sana Queue
 * 
 * @author Sana Development Team
 *
 */
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 4847187771370210197L;
    
    private Log log = LogFactory.getLog(this.getClass());
    
    public static class Params{
    	public static final String DESCRIPTION = "description"; 
    }
    


    /**
     * Called by the server to handle POST requests to this module.
     * 
     * POST requests must have the following fields:
     * <ul>
     * <li><b>description</b>JSON encoded encounter data</li> 
     * <li><b>[medImageFile-$element-$index.$ext,]</b>One or more file fields 
     * where the following come from the original Procedure xml
     *  	<ul>
     *  	<li>$element is the 'id' attribute value</li>
     *    	<li>$index is one of the csv's from the 'answer' attribute</li>
     *    	</ul>
     * </li>
     * </ul>
     * 
     * The 'description' field is parsed into an MDSMessage object consisting of
     * <ul>
     * <li><b>phoneId</b> the phone number of the client. Will be used for any 
     * 		response notifications</li>
     * <li><b>procedureDate</b> the date when the data was collected</li>
     * <li><b>procedureTitle</b> the title as visible in the Queue</li>
     * <li><b>patientId</b> a unique patient identifier</li>
     * <li><b>caseIdentifier</b> a client assigned UUID</li>
     * <li><b>responses</b> Encounter data which will be stored in OpenMRS as
     * 		Observations.</li>
     * </ul> 
     * 
     * <b>Note:<b> The Procedure xml format will of the 1.x branches will be 
     * changed in upcoming versions.
     */
    @Override
    protected void doPost(HttpServletRequest request, 
    		HttpServletResponse response) throws ServletException, IOException 
    {
        log.info("MDSUploadServlet Got post");
        MDSMessage message;
        Patient patient = null;
        Date encounterDateTime = null;
		Encounter encounter = null;
        Map<String, Concept> idMap = null;
		Set<Obs> observations = null;
		QueueItem queueItem;
		
		// grab any files from the request
        List<FileItem> files = Collections.emptyList();
        try {
            files = getUploadedFiles(request);
        } catch(APIException ex) {
            MDSResponse.fail(request, response, ex.getMessage(), log);
            return;
        }
        
        //TODO move this into a method so that we can handle the xml changes
        String jsonDescription = request.getParameter("description");
        if(jsonDescription == null) { 
        	// If we can't find it in the parameters, then we need to search 
        	// for it in the files list.
        	for (int i = 0; i < files.size(); ++i) {
        		FileItem f = files.get(i);
        		if (f == null)
        			continue;
        		if ("description".equals(f.getFieldName())) {
        			jsonDescription = f.getString();
        			files.remove(i);
        			break;
        		}
        	}
            // If still null, fail
        	if (jsonDescription == null) {
        		MDSResponse.fail(request,response, 
        				"Invalid description for encounter",log);
        		return;
        	}
        }
        // Convert to MDSMessage
        try {
        	Gson gson = new Gson();
        	message = gson.fromJson(jsonDescription, MDSMessage.class);
        } catch (com.google.gson.JsonParseException ex) {
        	MDSResponse.fail(request, response, 
        			"Error parsing MDSMessage: " + ex, log);
        	return;
        }

		// First check if it exists
		String uuid = request.getParameter("uuid");
		if(uuid != null)
			queueItem = Context.getService(QueueItemService.class)
								.getQueueItemByUuid(uuid);
        try{
            patient = getPatient(message.patientId);
        	if(patient == null){
        		throw new PatientIdentifierException("caseIdentifier:" 
        			+ message.caseIdentifier + ", Invalid patient id: " 
        			+ message.patientId);
        	} else 
        		log.debug("Sana.UploadServlet.doPost: caseIdentifier:" 
        			+message.caseIdentifier+", has patient "+message.patientId);
        } catch(PatientIdentifierException ex){	 
			MDSResponse.fail(request, response, ex.getMessage(), log);
			return;
       	 
		}
        
        String mid = message.caseIdentifier;
        // Only need this when debugging
        if(log.isDebugEnabled())
        	System.out.println("Sana.UploadServlet.doPost(): message: " + message);
     
        for(MDSQuestion q : message.questions)
        	System.out.println("Sana.UploadServlet.doPost: message("+mid+
        			"): question:"+q);

        // Validate before we persist anything to the database when creating 
        // the Encounter or Observations       
        String pattern = Context.getAdministrationService()
    			.getGlobalProperty(Property.DATE_FORMAT);
        
        try {
            // Check the date format
            encounterDateTime = new SimpleDateFormat(pattern).parse(message.procedureDate);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	MDSResponse.fail(request, response, 
        			"date: " + message.procedureDate, log);
            return;
        }
        try{
            // Validate the concepts
            idMap = makeIdToConceptMap(message);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	MDSResponse.fail(request, response, 
        			"Concept error: " + ex.getMessage(), log);
            return;
        }
        
		try{
			// translate the response into an encounter
			encounter = makeEncounter(patient, message, encounterDateTime, files);
		} catch(Exception ex){
			MDSResponse.fail(request, response, ex.getMessage(), log);
			return;
		}
		try{
			// Create the observations
			observations = makeObsSet(encounter, patient, message, files,
				encounterDateTime, idMap);

		} catch(Exception ex){
			MDSResponse.fail(request, response, ex.getMessage(), log);
			return;
		}
			// This constructs the queue item and saves it
		try{
			QueueItem q = makeQueueItem(encounter, patient, message);
			Context.getService(QueueItemService.class).saveQueueItem(q);

			MDSResponse.succeed(request, response, "Successfully uploaded "
					+ "procedure " + message.caseIdentifier, log);
		} catch (Exception ex) {
			ex.printStackTrace();
			StackTraceElement init = ex.getStackTrace()[0];
			log.error("Upload Error: " + ex.toString() + ", source: "
					+ init.toString() + ", method: " + init.getMethodName()
					+ " at line no. : " + init.getLineNumber());
			MDSResponse.fail(request, response, ex.getMessage(), log);
			return;
		}
	}
    
    
    /**
     * Returns zero or one patient from the OpenMRS Patient service looked up
     * by the patient identifier
     * 
     * @param patientIdentifier the id to look up
     * @return a patient with a matching identifier as produced by 
     * 			Patient.getIdentifier() or null
     */
    private Patient getPatient(String patientIdentifier) {
        Patient patient = null;
    	// Safety check
        if(patientIdentifier == null || "".equals(patientIdentifier)) 
        	throw new PatientIdentifierException("Null patient id");
        
        PatientService patientService = Context.getPatientService();
        List<Patient> patients = patientService.getPatients(null, 
        		patientIdentifier, null, false);
        
        if(patients.size() > 0)
            for(Patient p : patients) {
        	if(p.getPatientIdentifier().getIdentifier().equals(patientIdentifier))
        		patient = p;
        		break;
            }
        else throw new PatientIdentifierException("Invalid patient id: " 
        			+ patientIdentifier);
        return patient;
    }
    
    private Patient getPatientByUuid(String uuid) {
        Patient patient = null;
    	// Safety check
        if(uuid.isEmpty()) 
        	throw new APIException("Null patient id");
        
        return Context.getPatientService().getPatientByUuid(uuid);
    }
    
    private Encounter getEncounterByUuid(String uuid) {
    	// Safety check
        if(uuid.isEmpty()) 
        	throw new APIException("Null encounter id");
        return Context.getEncounterService().getEncounterByUuid(uuid);
    }
    
    @Override
    protected void doPut(HttpServletRequest request, 
    		HttpServletResponse response) throws ServletException, IOException 
    {

		
    	
    }
    /**
     * Gets any files uploaded with this request.
     * @param request the original request.
     * @return A list of FileItem objects.
     * @throws FileUploadException 
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    private List<FileItem> getUploadedFiles(HttpServletRequest request) throws 
    	APIException 
    {   
    	List<FileItem> files = Collections.emptyList();
    	try{
        	FileItemFactory factory = new DiskFileItemFactory();
        	ServletFileUpload upload = new ServletFileUpload(factory);
        	if(!request.getHeader("content-type").contains(
        		"application/x-www-form-urlencoded"))        
        		files = (List<FileItem>)upload.parseRequest(request);
    	} catch(FileUploadException ex) {
    		throw new APIException("File POST Error",ex);
    	}
    	return files;
    }
    
    private Patient getPatientObject(String uuid){
    	Patient p =  Context.getPatientService().getPatientByUuid(uuid);
    	/*
		description.addRequiredProperty("patient");
		*/
    	return p;
    }
    
    
    private Encounter createEncounter(Patient patient, Date encounterDateTime,
    		Location location, EncounterType type, Provider provider,
    		Concept question
    		){
    	/*
		description.addRequiredProperty("encounterDatetime");
		description.addRequiredProperty("patient");
		description.addRequiredProperty("encounterType");
		
		description.addProperty("location");
		description.addProperty("form");
		description.addProperty("provider");
		description.addProperty("orders");
		description.addProperty("obs");
		*/

        Encounter e = new Encounter();
        e.setPatient(patient);
        e.setEncounterDatetime(encounterDateTime);
        e.setDateCreated(new Date());
        e.setCreator(Context.getAuthenticatedUser());
        e.setProvider(Context.getAuthenticatedUser().getPerson());
        e.setLocation(location);
        
        // TODO(XXX) Replace these, catch exceptions, etc.
        e.setForm(Context.getFormService().getAllForms().get(0));
        e.setEncounterType(Context.getEncounterService().getAllEncounterTypes()
        		.get(0));
        Context.getEncounterService().saveEncounter(e);
        Integer encounterId = e.getId();
        Context.evictFromSession(e);
    	return Context.getEncounterService().getEncounter(encounterId);
    }
    
    Map<String,List<FileItem>> parseFileObs(MDSMessage message, 
    		List<FileItem> files)
    {
    	Map<String,List<FileItem>> fileMap = new HashMap<String,List<FileItem>>();
    	log.debug("Sana.UploadServlet.parseFileObs(): file count: "
				+ ((files != null)? files.size(): "EMPTY"));
    	for(FileItem f : files) {
            // Look for the form: medImageFile-id-number
            // TODO(XXX) we could run into trouble with this later if someone 
        	// puts a '-' into the EID
            String[] parts = f.getFieldName().split("-");
            assert(parts.length == 3);
            assert(parts[0].equals("medImageFile"));
            
            String eid = parts[1];
            if(fileMap.containsKey(eid)) {
                fileMap.get(eid).add(f);
                log.info("Upload case: " + message.caseIdentifier 
                		+ ", element: " +eid 
                		+", has file: "+f.getFieldName());
            } else {
                List<FileItem> fileList = new ArrayList<FileItem>();
                fileList.add(f);
                fileMap.put(eid, fileList);
            }
        }
        return fileMap;
    }
    
    /** 
     * @deprecated
     */
    Set<Obs> makeObsSet(Encounter encounter, Patient patient, MDSMessage message, 
    		List<FileItem> files, Date date) throws IOException
    {
    	Set<Obs> observations = new HashSet<Obs>();
        Map<String,List<FileItem>> fileMap = parseFileObs(message, files);
		log.debug("Sana.UploadServlet.createObsSet(): file count: "
				+ ((fileMap != null)? fileMap.size(): "EMPTY"));
		
    	// Create the observations
        for(MDSQuestion q : message.questions) {
        	Obs obs = null;
            // Get Concept for Type/ID
            Concept c = getOrCreateConcept(q.type, q.concept, q.question);
            boolean isComplex = c.isComplex();
            
            // 1.x versions allow multiple files per question so we need to
            // make one observation per file
            if(isComplex) {
                // If no file was uploaded, then there is no obs to make.
                if(fileMap.containsKey(q.id)) {
                    // Make one obs per file
                    for(FileItem f : fileMap.get(q.id)) {
                    	obs = makeObs(encounter,patient,date,c, q, f);
                    }
                } else {
                	// No observation if no files
                	obs = null;
                }
            } else {
                // Not complex, make a regular obs 
            	obs = makeObs(encounter, patient,date,c, q, null);
            }
            if (obs != null){
            	log.debug("Sana.UploadServlet.createObsSet():"
        				+ "Eid: " + q.id + ", Concept: " + q.concept 
        				+ ", Obs created. Complex = "
        				+ ((obs.getComplexData() != null)? obs.getValueComplex(): "False"));
            	observations.add(obs);
            } else
        		log.debug("Sana.UploadServlet.createObsSet():"
        				+ "Eid: " + q.id + ", Concept: " + q.concept 
        				+ ", Obs not created. Complex type No FIle");
        }
        return observations;
    }
    
    /**
     * Parses an MDSMessage, validates the Concepts and returns a Map of the
     * question ids to Concept objects
     * 
     * @param message The message to parse
     * @return a mapping of question id's to Concept objects
     */
    Map<String, Concept> makeIdToConceptMap(MDSMessage message)
    {
    	Map<String,Concept> idMap = new HashMap<String,Concept>();
    	// validate valid Concepts and map to id
        for(MDSQuestion q : message.questions) {
            // Get Concept for Type/ID
            Concept c = getOrCreateConcept(q.type, q.concept, q.question);
            idMap.put(q.id, c);
        }
    	return idMap;
    }
    
    /**
     * Creates an observation set for an associated encounter from the data
     * attached to a POST call as a list of files and MDS message. The concepts
     * must be validated and passed as a map from the id to Concept.
     * 
     * @return
     * @throws IOException
     */
    Set<Obs> makeObsSet(Encounter encounter, Patient patient, MDSMessage message, 
    		List<FileItem> files, Date date, Map<String, Concept> idMap) 
    		throws IOException
    {
    	Set<Obs> observations = new HashSet<Obs>();
        Map<String,List<FileItem>> fileMap = parseFileObs(message, files);
    	// Create the observations
        for(MDSQuestion q : message.questions) {
        	Obs obs = null;
            Concept c = idMap.get(q.id);
            boolean isComplex = c.isComplex();
            
            // 1.x versions allow multiple files per question so we need to
            // make one observation per file
            if(isComplex) {
                // If no file was uploaded, then there is no obs to make.
                if(fileMap.containsKey(q.id)) {
                    // Make one obs per file
                    for(FileItem f : fileMap.get(q.id)) {
                    	obs = makeObs(encounter,patient,date,c, q, f);
                    }
                } else {
                	// No observation if no files
                	obs = null;
                }
            } else {
                // Not complex, make a regular obs 
            	obs = makeObs(encounter, patient,date,c, q, null);
            }
            if (obs != null){
            	log.debug("Sana.UploadServlet.createObsSet():"
        			+ "Eid: " + q.id + ", Concept: " + q.concept 
        			+ ", Obs created. Complex = "
        			+ ((obs.getComplexData() != null)? obs.getValueComplex(): "False"));
            	observations.add(obs);
            } else
        		log.debug("Sana.UploadServlet.createObsSet():"
        			+ "Eid: " + q.id + ", Concept: " + q.concept 
        			+ ", Obs not created. Complex type No File");
        }
        return observations;
           
    }
    
    /**
     * Takes an MDSmessage and creates Observations and Encounter in OpenMRS
     * 
     * @param patient The subject of the data collection
     * @param message The MDSMessage containing the encounter text
     * @param procedureDate The date of the encounter
     * @param files Files collected as part of the procedure
     * @throws IOException 
     */
    Encounter makeEncounter(Patient patient, MDSMessage message, Date procedureDate, 
    	List<FileItem> files) throws IOException, PatientIdentifierException
    {
        // Now we assemble the encounter
        Encounter e = new Encounter();
        e.setPatient(patient);
        e.setEncounterDatetime(procedureDate);
        e.setDateCreated(new Date());
        e.setCreator(Context.getAuthenticatedUser());
        e.setProvider(Context.getAuthenticatedUser().getPerson());
        Location location = Context.getLocationService().getDefaultLocation();
        e.setLocation(location);
        
        // TODO(XXX) Replace these, catch exceptions, etc.
        e.setForm(Context.getFormService().getAllForms().get(0));
        e.setEncounterType(Context.getEncounterService().getAllEncounterTypes()
        		.get(0));
        Context.getEncounterService().saveEncounter(e);
        Integer encounterId = e.getId();
        Context.evictFromSession(e);
    	return Context.getEncounterService().getEncounter(encounterId);
    	//return e;
    }
    
    QueueItem makeQueueItem(Encounter e, Patient p, MDSMessage message){
    	QueueItem q = new QueueItem();
    	q.setStatus(QueueItemStatus.NEW);
        //q.setPhoneIdentifier(message.phoneId);
        //q.setCaseIdentifier(message.caseIdentifier);
        //q.setProcedureTitle(message.procedureTitle);
        q.setCreator(Context.getAuthenticatedUser());
        q.setDateCreated(new Date());
        q.setChangedBy(Context.getAuthenticatedUser());
        q.setDateChanged(new Date());
        q.setDateCreated(new Date());
        q.setEncounter(e);
        return q;
    }
    
    //TODO Do we want encounter uploads to trigger concept creation? This can 
    // cause duplicate and erroneous concepts loaded into the database.
    // Temporary hack until we get the Android XML format to have more flexibility.
    /**
     * Fetches or creates a Concept from the OpenMRS data store.
     *
     * @param m The mds message from an originating request.
     * @param eid The 'id' attribute of the Procedure xml element.
     * @param type The 'type' attribute of the Procedure xml element.
     * @param name The 'concept' attribute of the Procedure xml element.
     * 		which should map to a 'Name' field for an existing OpenMRS
     * 		Concept.
     * @param question The 'question' attribute of the Procedure xml element
     * 		which should map to a 'description' field for an existing OpenMRS
     * 		Concept.
     * @return A valid OpenMRS Concept or null.
     */
    private Concept getOrCreateConcept(MDSMessage m, String type, 
    		String name, String question) 
    {    
        ConceptService cs = Context.getConceptService();
        Concept c = null;
		// Search for a concept named conceptName

		// Get concepts matching these words in this locale
        Locale defaultLocale = Context.getLocale();
		List<ConceptWord> conceptWords = new Vector<ConceptWord>();
		conceptWords.addAll(cs.getConceptWords(name, defaultLocale));
		log.debug("Found Concept words with matching name:" 
					+ conceptWords.toString());
		// Use the description field to uniquely match 
		for(ConceptWord cw : conceptWords){
			try {
				log.debug("Sana.UploadServlet.getOrCreateConcept():Testing: (" 
						+name + ", " + question + " ) " 
						+ " against ConceptWord:  (" + cw.getConceptName() + ", " 
						+ cw.getConcept().getDescription().getDescription() 
						+ " ) ");
				// stop checking if there is a match
				if(cw.getConcept().getDescription().getDescription()
						.equals(question))
				{
					log.debug("Sana.UploadServlet.Concept Matched: (" + name + ", " 
							+ question + " ) id: " + cw.getConcept().getId());
					c = cw.getConcept();
					break;
				}
			} catch(Exception err){
				log.debug("Sana.UploadServlet.getOrCreateConcept(): Skipping concept " 
						+ name + " because of error: " + err.toString());
				err.printStackTrace();
			}
		}
		
		//c = cs.getConceptByName(conceptName);
		// If we get a null concept here we need to see if we can create it
		if(c == null){
			String msg = String.format("Concept Undefined: (%s, %s)", 
					name, question);
			log.error(msg);
			boolean allowCreate = Boolean.valueOf(
				Context.getAdministrationService().getGlobalProperty(
							ModuleConstants.PROP_ALLOW_CONCEPT_CREATE));
			if(allowCreate){
				c = createAndGetConcept(name, type, question);
			} else
				throw new NullPointerException(msg);
		}
		
		log.debug("Concept found: " + c.getDisplayString());
        return c;
    }
    
    /** Convenience wrapper */
    private Concept getOrCreateConcept(String type,  String name, String desc){
    	return getOrCreateConcept(null,type,name,desc);
    }
    
    /**
     * Constructs a new Observation
     * @param p The subject of the observation
     * @param c The Observation concept
     * @param q the question attribute from the procedure xml
     * @param f a file item associated with the observation
     * @return A new Obs 
     * @throws IOException
     * 
     * @deprecated
     */
    private Obs makeObs(Patient p, Concept c, MDSQuestion q, FileItem f) 
    	throws IOException 
    {
        Obs o = new Obs();
        
        o.setCreator(Context.getAuthenticatedUser());
        o.setDateCreated(new Date());
        o.setPerson(p);
        Location location = Context.getLocationService().getDefaultLocation();
        o.setLocation(location);
        o.setConcept(c);
        o.setValueText(q.answer);
        if(f == null)
        	o.setValueText(q.answer);
        else
        	o.setValueText(q.type);
        
        if(f != null) {
        	log.debug("Sana.UploadServlet.makeObs: file:" + f.getName());
            ComplexData cd = new ComplexData(f.getName(), f.getInputStream());
            o.setComplexData(cd);
        }
        return o;
    }
    
    /**
     * Connstructs a new Observation. And saves it in the database. 
     * 
     * This version of makeObs is intended to be forward compatible, OpenMRS
     * ver. greater than 1.6, by handling modifications to the OpenMRS 
     * hibernation mechanism.
     * 
     * @param p The subject of the observation
     * @param date The date of the observation 
     * @param c The Observation concept
     * @param q the question attribute from the procedure xml
     * @param f a file item associated with the observation
     * @return A new Obs 
     * @throws IOException
     */
    private Obs makeObs(Encounter encounter, Patient p, Date date, Concept c, MDSQuestion q, 
    		FileItem f) throws IOException 
    {
        System.out.println("Sana.UploadServlet.makeObs(): q=" +
        		q.toString());

        System.out.println("Sana.UploadServlet.makeObs(): p=" +
        		p.printAttributes());

        System.out.println("Sana.UploadServlet.makeObs(): c=" +
        		c.getUuid() + "::" + c.getDisplayString());
        Obs o = new Obs(p, c, date,
        			Context.getLocationService().getDefaultLocation());
        o.setCreator(Context.getAuthenticatedUser());
        o.setEncounter(encounter);
        if(f == null)
        	o.setValueText(q.answer);
        else
        	o.setValueText(q.type);
        
        if(f != null) {
        	log.debug("Sana.UploadServlet.makeObs(): file:" + f.getName());
            ComplexData cd = new ComplexData(f.getName(), f.getInputStream());
            o.setComplexData(cd);
        }
        System.out.println("Sana.UploadServlet.makeObs(): " +
        		o.getUuid() +"::"+o.toString());
        Context.getObsService().saveObs(o, "");
        //Integer id = o.getObsId();
        //Context.evictFromSession(o);
        //return Context.getObsService().getObs(id);
        return o;
    }
    
    protected Concept createAndGetConcept(String name, String type, String question)
    {
    	Concept c = null;
        // TODO make this constant / static somewhere
        if((name == null && type == null) || question == null){
        	String msg = String.format("Could not create: (%s, %s, %s)", 
					name, type, question);
        	log.error(msg);
			throw new NullPointerException(msg);
        }
        	
    	Map<String,String> typeMap = new HashMap<String,String>();
        typeMap.put("PLUGIN", "Complex");
        typeMap.put("PICTURE", "Complex");
        typeMap.put("SOUND", "Complex");
        typeMap.put("VIDEO", "Complex");
        typeMap.put("BINARYFILE", "Complex");
        typeMap.put("TEXT", "Text");
        typeMap.put("ENTRY", "Text");
        typeMap.put("SELECT", "Text");
        typeMap.put("MULTI_SELECT", "Text");
        typeMap.put("RADIO", "Text");
        typeMap.put("GPS", "Text");
        typeMap.put("INVALID", "Text");
        typeMap.put("PATIENT_ID", "Text");
        
        Map<String,String> handlerMap = new HashMap<String,String>();
        handlerMap.put("PICTURE", "ThumbnailingImageHandler");
        handlerMap.put("SOUND", "MediaFileHandler");
        handlerMap.put("VIDEO", "MediaFileHandler");
        handlerMap.put("BINARYFILE", "MediaFileHandler");
        handlerMap.put("PLUGIN", "MediaFileHandler");

        String typeName = typeMap.get(type);
        
        if (typeName == null) {
        	typeName = "Text";
        }
        ConceptDatatype conceptType = Context.getConceptService()
        								.getConceptDatatypeByName(typeName);
        // Shouldn't happen
        if(conceptType == null)
            return null;
        ConceptClass conceptClass = Context.getConceptService()
        								.getConceptClassByName("Question");
        // Shouldn't happen
        if(conceptClass == null)
            return null;
        
        // Start building the Concept
        if(typeName.equals("Complex")) {
        	String handler = handlerMap.get(type);
        	if (handler == null) {
        		// Eep! Fall back on a basic file handler
        		handler = "FileHandler";
        	}
        	c = new ConceptComplex(null, handler); // I feel dirty.
        } else {
            c = new Concept();
        }
        // Concept fields
        c.setDatatype(conceptType);            
        c.setConceptClass(conceptClass);
        c.setCreator(Context.getAuthenticatedUser());
        c.setDateCreated(new Date());
        
        ConceptNameTag preferredTag = Context.getConceptService()
        			.getConceptNameTagByName(ConceptNameTag.PREFERRED);
        // Make sure it is available in all locales supported by the server
        List<Locale> locales = Context.getAdministrationService()
        			.getAllowedLocales();
		for (Locale locale : locales) {
			// concept attribute from procedure xml
			ConceptName lc = new ConceptName(name, locale);
            lc.addTag(preferredTag);
            c.addName(lc);
			// question attribute from procedure xml
            ConceptDescription description = new ConceptDescription(
            		question, locale);
            description.setCreator(Context.getAuthenticatedUser());
            c.addDescription(description);
		}
        Context.getConceptService().saveConcept(c);
        return c;
    }
    
    //TODO Use this to look up encounters?
    // for testing 
    @Override
    protected void doGet(HttpServletRequest request, 
    		HttpServletResponse response) throws ServletException, IOException 
    {
        MDSResponse.succeed(request, response, 
        		"Hello World from UploadServlet", log);
    }
}


