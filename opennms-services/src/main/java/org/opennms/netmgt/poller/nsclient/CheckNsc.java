//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2006 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
//      OpenNMS Licensing       <license@opennms.org>
//      http://www.opennms.org/
//      http://www.opennms.com/
//
package org.opennms.netmgt.poller.nsclient;

import org.opennms.netmgt.poller.nsclient.NsclientCheckParams;
import org.opennms.netmgt.poller.nsclient.NsclientException;
import org.opennms.netmgt.poller.nsclient.NsclientManager;
import org.opennms.netmgt.poller.nsclient.NsclientPacket;
import java.util.ArrayList;

/**
 * This is an example commandline tool to perform checks against NSClient
 * services using <code>NsclientManager</code>
 * 
 * @author <A HREF="mailto:matt.raykowski@gmail.com">Matt Raykowski </A>
 * @author <A HREF="http://www.opennsm.org">OpenNMS </A>
 */
public class CheckNsc {

    /**
     * @param args
     *            args[0] must contain the remote host name args[1] must
     *            contain the check name (e.g. CLIENTVERSION) args[2] (crit)
     *            and args[2] (warn) must contain a numeric value args[4] must
     *            contain an empty string or a parameter related to the check
     */
    public static void main(String[] args) {
        ArrayList arguments = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            arguments.add(args[i]);
        }

        if (arguments.size() < 2) {
        	usage();
        	System.exit(1);
        }
        
        String  host         = (String)arguments.remove(0);
        String  command      = (String)arguments.remove(0);
        int warningLevel     = 0;
        int criticalLevel    = 0;
        String  clientParams = "";
        
        if (!arguments.isEmpty()) {
        	warningLevel  = Integer.parseInt((String)arguments.remove(0));
        }
        
        if (!arguments.isEmpty()) {
        	criticalLevel = Integer.parseInt((String)arguments.remove(0));
        }

        /* whatever's left gets merged into "arg1&arg2&arg3" */
        if (!arguments.isEmpty()) {
        	for (int i=0; i < arguments.size(); i++) {
        		clientParams += arguments.get(i);
        		if (i < (arguments.size() - 1)) {
        			clientParams += "&";
        		}
        	}
        }
        
        int port = 1248;
        
        if (host.indexOf(":") >= 0) {
        	port = Integer.parseInt(host.split(":")[1]);
        	host = host.split(":")[0];
        }

        NsclientManager client = null;
        NsclientPacket response = null;
        NsclientCheckParams params = null;
        
        try {
        	client = new NsclientManager(host, port);
        }
        catch (Exception e) {
        	usage("An error occurred creating a new NsclientManager.", e);
        }

        try {
        	client.setTimeout(5000);
        	client.init();
        }
        catch (Exception e) {
        	usage("An error occurred initializing the NsclientManager.", e);
        }

        try {
        	params = new NsclientCheckParams( warningLevel, criticalLevel, clientParams);
        }
        catch (Exception e) {
        	usage("An error occurred creating the parameter object.", e);
        }

        try {
        	response = client.processCheckCommand(
                                              NsclientManager.convertStringToType(command),
                                              params);
        }
        catch(Exception e) {
        	usage("An error occurred processing the command.", e);
        }
        
        if (response == null) {
        	usage("No response was returned.", null);
        } else {
            System.out.println("NsclientPlugin: "
                    + command
                    + ": "
                    + NsclientPacket.convertStateToString(response.getResultCode()) /* response.getResultCode() */
                    + " (" + response.getResponse() + ")");
        }
    }

	private static void usage() {
		usage(null, null);
	}

    private static void usage(String message, Exception e) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("usage: CheckNsc <host>[:port] <command> [[warning level] [critical level] [arg1..argn]]\n");
    	sb.append("\n");
    	sb.append("  host:           the hostname to connect to (and optionally, the port)\n");
    	sb.append("  command:        the command to run against NSClient\n");
    	sb.append("  warning level:  warn if the level is above X\n");
    	sb.append("  critical level: error if the level is above X\n");
    	sb.append("\n");
    	sb.append("  All subsequent arguments are considered arguments to the command.\n\n");
    	
    	if (e != null) {
    		sb.append("In addition, an exception occurred:\n");
    		sb.append(message).append("\n");
    		sb.append(e.getStackTrace()).append("\n\n");
    	} else if (message != null) {
    		sb.append("Error: " + message + "\n\n");
    	}
    	
    	System.out.print(sb);
    }
    
}
