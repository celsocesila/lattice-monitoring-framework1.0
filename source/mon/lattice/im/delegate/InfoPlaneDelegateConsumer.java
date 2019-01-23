/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.im.delegate;

import mon.lattice.core.ID;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;

/**
 *
 * @author uceeftu
 */
public interface InfoPlaneDelegateConsumer {
    public boolean containsDataSource(ID id);
        
    public boolean containsDataConsumer(ID id);
    
    public JSONArray getDataSources() throws JSONException;
    
    public JSONArray getDataConsumers() throws JSONException;
    
    public ControlEndPointMetaData getDSAddressFromProbeID(ID probe) throws ProbeNotFoundException, DSNotFoundException;
    
    public ControlEndPointMetaData getDSAddressFromID(ID dataSource) throws DSNotFoundException;
    
    public String getDSIDFromName(String dsName) throws DSNotFoundException;
    
    public ControlEndPointMetaData getDCAddressFromID(ID dataConsumer) throws DCNotFoundException;
    
    public ControlEndPointMetaData getDCAddressFromReporterID(ID reporter) throws ReporterNotFoundException, DCNotFoundException;
    
    public Integer getDSPIDFromID(ID dataSource) throws DSNotFoundException;
    
    public int getDCPIDFromID(ID dataConsumer) throws DCNotFoundException;  
    
    public JSONArray getProbesOnDS(ID dataSource) throws DSNotFoundException; 
}