package org.openmrs.module.sana.queue.extension.html;

import java.util.HashMap;
import java.util.Map;

import org.openmrs.module.Extension;
import org.openmrs.module.sana.ModuleConstants.Module;
import org.openmrs.module.web.extension.AdministrationSectionExt;
// OpenMRS 1.6.x 
// import org.openmrs.module.extension.AdministrationSectionExt;

/**
 * Creates the entries for the module on the Administration page
 * 
 * @author Sana Development Team
 */
public class AdminList extends AdministrationSectionExt {

    public Extension.MEDIA_TYPE getMediaType() {
        return Extension.MEDIA_TYPE.html;
    }
    
    /**
     * The String title which will be placed in the Administration section
     */
    public String getTitle() {
        return Module.ID+".title";
    }
    
    /**
     * Links to sub entries in the admin section
     */
    public Map<String, String> getLinks() {
        
        Map<String, String> map = new HashMap<String, String>();
        
        map.put("module/"+Module.ID+"/queue.form", Module.ID+".view");
        map.put("module/"+Module.ID+"/lexicon.form", Module.ID+".lexicon");
        map.put("module/"+Module.ID+"/mdsLog.form", Module.ID+".mdsLog");
        map.put("module/"+Module.ID+"/mdsAdmin.form", Module.ID+".mdsAdmin");
    	map.put("module/"+Module.ID+"/mediaViewer.form",
    					Module.ID+".admin_page_module_name");
        return map;
    }
}
