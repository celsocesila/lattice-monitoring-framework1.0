/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mon.lattice.appl.reporters;

import mon.lattice.core.AbstractReporter;
import mon.lattice.core.Measurement;
import mon.lattice.core.ProbeValue;
import mon.lattice.core.ProbeValueWithName;
import mon.lattice.core.Timestamp;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.web.Resty;
import static us.monoid.web.Resty.form;

/**
 *
 * Save the measurements in the InfluxDB database
 */
public class InfluxDBReporter extends AbstractReporter {
    String serverAddress;
    String serverPort;
    String influxDBURI;
    String database;
    
    Resty resty = new Resty();
    
    private Logger LOGGER = LoggerFactory.getLogger(InfluxDBReporter.class);
    
    
    public InfluxDBReporter(String address, String port, String database) {
        super("InfluxDBReporter");
        this.serverAddress = address;
        this.serverPort = port;
        this.database= database;
        this.influxDBURI = "http://" + serverAddress + ":" + serverPort + "/write?db=" + database + "&precision=ms";
        
        LOGGER.info("URI: " + influxDBURI );
        // should check it the DB exists and create it in case        
    }
    
    @Override
    public void report(Measurement m) {
        //should create a buffer and flushing it every N received measurements
        LOGGER.info("Received measurement: " + m.toString());
        
        Timestamp timestamp = m.getTimestamp();
        
        StringBuilder formattedMeasurement = new StringBuilder(); 
        
        Iterator<ProbeValue> values = m.getValues().iterator();
        String resourceId = (String)values.next().getValue();
        
        while (values.hasNext()) {
            ProbeValue attribute = values.next();
            formattedMeasurement.append(((ProbeValueWithName)attribute).getName())
                                .append("," + "serviceid=")
                                .append(m.getServiceID())
                                .append("," + "resourceid=")
                                .append(resourceId)
                                .append(" " + "value=")
                                .append(attribute.getValue())
                                .append(" ")
                                .append(timestamp)
                                .append("\n");
            }
        
        LOGGER.info(formattedMeasurement.toString());
        
        try {
            resty.json(influxDBURI, form(formattedMeasurement.toString()));
        } catch (IOException e) {
            LOGGER.error("Error while writing measurement to the DB: " + e.getMessage());
            for (int i=0; i< e.getStackTrace().length; i++)
                LOGGER.error(e.getStackTrace()[i].toString());
        }
    }
    
}
