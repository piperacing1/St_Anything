/**
 *  Parent_ST_Anything_Ethernet2.groovy
 *
 *  Copyright 2017 Dan G Ogorchock 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    2017-02-08  Dan Ogorchock  Original Creation
 *    2017-02-12  Dan Ogorchock  Modified to work with Ethernet based devices instead of ThingShield
 *    2017-02-24  Dan Ogorchock  Created the new "Multiples" device handler as a new example
 *    2017-04-16  Dan Ogorchock  Updated to use the new Composite Device Handler feature
 *    2017-06-10  Dan Ogorchock  Added Dimmer Switch support
 *    2017-07-09  Dan Ogorchock  Added number of defined buttons tile
 *    2017-08-24  Allan (vseven) Change the way values are pushed to child devices to allow a event to be executed allowing future customization
 *    2007-09-24  Allan (vseven) Added RGB LED light support with a setColorRGB routine
 *    2017-10-07  Dan Ogorchock  Cleaned up formatting for readability
 *    2017-09-24  Allan (vseven) Added RGBW LED strip support with a setColorRGBW routine
 *    2017-12-29  Dan Ogorchock  Added WiFi RSSI value per request from ST user @stevesell
 *    2018-02-15  Dan Ogorchock  Added @saif76's Ultrasonic Sensor
 *    2018-02-25  Dan Ogorchock  Added Child Presence Sensor
 *    2018-03-03  Dan Ogorchock  Added Child Power Meter
 *	
 */
 
metadata {
	definition (name: "Parent_ST_Anything_Ethernet2", namespace: "ogiewon", author: "Dan Ogorchock") {
        capability "Configuration"
        capability "Refresh"
        capability "Button"
        capability "Holdable Button"
        capability "Signal Strength"   
	}

    simulator {
    }

    // Preferences
	preferences {
		input "ip", "text", title: "ESP8266 IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
		input "port", "text", title: "ESP8266 Port", description: "port in form of 8090", required: true, displayDuringSetup: true
		input "mac", "text", title: "ESP8266 MAC Address", description: "MAC Address in form of 02A1B2C3D4E5", required: true, displayDuringSetup: true
		input "numButtons", "number", title: "Number of Buttons", description: "Number of Buttons, 0 to n", required: true, displayDuringSetup: true
	}

	// Tile Definitions
	tiles (scale: 2){
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'Refresh', action: "refresh.refresh", icon: "st.secondary.refresh-icon"
		}
        
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "configure", label:'Configure', action:"configuration.configure", icon:"st.secondary.tools"
		}

        valueTile("numberOfButtons", "device.numberOfButtons", inactiveLabel: false, width: 2, height: 2) {
			state "numberOfButtons", label:'${currentValue} buttons', unit:""
		}

        valueTile("rssi", "device.rssi", width: 2, height: 2) {
			state("rssi", label:'RSSI ${currentValue}', unit:"",
				backgroundColors:[
					[value: -30, color: "#006600"],
					[value: -45, color: "#009900"],
					[value: -60, color: "#99cc00"],
					[value: -70, color: "#ff9900"],
					[value: -90, color: "#ff0000"]
				]
			)
		}

		childDeviceTiles("all")
	}
}

// parse events into attributes
def parse(String description) {
	//log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
	def headerString = msg.header

	if (!headerString) {
		//log.debug "headerstring was null for some reason :("
    }

	def bodyString = msg.body

	if (bodyString) {
        log.debug "Parsing: $bodyString"
    	def parts = bodyString.split(" ")
    	def name  = parts.length>0?parts[0].trim():null
    	def value = parts.length>1?parts[1].trim():null
        
		def nameparts = name.split("\\d+", 2)
		def namebase = nameparts.length>0?nameparts[0].trim():null
        def namenum = name.substring(namebase.length()).trim()
		
        def results = []
        
		if (name.startsWith("button")) {
			//log.debug "In parse:  name = ${name}, value = ${value}, btnName = ${name}, btnNum = ${namemun}"
        	results = createEvent([name: namebase, value: value, data: [buttonNumber: namenum], descriptionText: "${namebase} ${namenum} was ${value} ", isStateChange: true, displayed: true])
			log.debug results
			return results
        }

		if (name.startsWith("rssi")) {
			//log.debug "In parse: RSSI name = ${name}, value = ${value}"
           	results = createEvent(name: name, value: value, displayed: false)
            log.debug results
			return results
        }


        def isChild = containsDigit(name)
   		//log.debug "Name = ${name}, isChild = ${isChild}, namebase = ${namebase}, namenum = ${namenum}"      
        //log.debug "parse() childDevices.size() =  ${childDevices.size()}"

		def childDevice = null

		try {

            childDevices.each {
				try{
            		//log.debug "Looking for child with deviceNetworkID = ${device.deviceNetworkId}-${name} against ${it.deviceNetworkId}"
                	if (it.deviceNetworkId == "${device.deviceNetworkId}-${name}") {
                	childDevice = it
                    //log.debug "Found a match!!!"
                	}
            	}
            	catch (e) {
            	//log.debug e
            	}
        	}
            
            //If a child should exist, but doesn't yet, automatically add it!            
        	if (isChild && childDevice == null) {
        		log.debug "isChild = true, but no child found - Auto Add it!"
            	//log.debug "    Need a ${namebase} with id = ${namenum}"
            
            	createChildDevice(namebase, namenum)
            	//find child again, since it should now exist!
            	childDevices.each {
					try{
            			//log.debug "Looking for child with deviceNetworkID = ${device.deviceNetworkId}-${name} against ${it.deviceNetworkId}"
                		if (it.deviceNetworkId == "${device.deviceNetworkId}-${name}") {
                			childDevice = it
                    		//log.debug "Found a match!!!"
                		}
            		}
            		catch (e) {
            			//log.debug e
            		}
        		}
        	}
            
            if (childDevice != null) {
                //log.debug "parse() found child device ${childDevice.deviceNetworkId}"
                
//                if (namebase == "temperature") {
//                	double tempC = fahrenheitToCelsius(value.toFloat())  //convert from Farenheit to Celsius
//                   	value = tempC.round(2)
//				}
                
//                if (namebase == "dimmerSwitch") { namebase = "switch"}  //use a "switch" attribute to maintain standards
//                childDevice.sendEvent(name: namebase, value: value)
                childDevice.generateEvent(namebase, value)
				log.debug "${childDevice.deviceNetworkId} - name: ${namebase}, value: ${value}"
                //If event was dor a "Door Control" device, also update the child door control device's "Contact Sensor" to keep everything in synch
//                if (namebase == "doorControl") {
//                	childDevice.sendEvent(name: "contact", value: value)
//                    log.debug "${childDevice.deviceNetworkId} - name: contact, value: ${value}"
//                }
            }
            else  //must not be a child, perform normal update
            {
                results = createEvent(name: name, value: value)
                log.debug results
                return results
            }
		}
        catch (e) {
        	log.error "Error in parse() routine, error = ${e}"
        }

	}
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port
    
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

def sendEthernet(message) {
	log.debug "Executing 'sendEthernet' ${message}"
	if (settings.ip != null && settings.port != null) {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: "/${message}?",
            headers: [ HOST: "${getHostAddress()}" ]
        ))
    }
    else {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")   
    }
}

// handle commands
def childAlarmOn(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmOn($dni), name = ${name}"
    sendEthernet("${name} both")
}

def childAlarmSiren(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmOn($dni), name = ${name}"
    sendEthernet("${name} siren")
}

def childAlarmStrobe(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmOn($dni), name = ${name}"
    sendEthernet("${name} strobe")
}

def childAlarmBoth(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmOn($dni), name = ${name}"
    sendEthernet("${name} both")
}

def childAlarmOff(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmOff($dni), name = ${name}"
    sendEthernet("${name} off")
}

def childAlarmTest(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childAlarmTest($dni), name = ${name}"
    sendEthernet("${name} both")
	runIn(3, childAlarmTestOff, [data: [devicenetworkid: dni]])
}

def childAlarmTestOff(data) {
	childAlarmOff(data.devicenetworkid)
}

void childDoorOpen(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childDoorOpen($dni), name = ${name}"
    sendEthernet("${name} on")
}

void childDoorClose(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childDoorClose($dni), name = ${name}"
    sendEthernet("${name} on")
}

void childOn(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childOn($dni), name = ${name}"
    sendEthernet("${name} on")
}

void childOff(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childOff($dni), name = ${name}"
    sendEthernet("${name} off")
}

void childSetLevel(String dni, value) {
    def name = dni.split("-")[-1]
    log.debug "childSetLevel($dni), name = ${name}, level = ${value}"
    sendEthernet("${name} ${value}")
}

void childSetColorRGB(String dni, value) {
    def name = dni.split("-")[-1]
    log.debug "childSetColorRGB($dni), name = ${name}, colorRGB = ${value}"
    sendEthernet("${name} ${value}")
}
// newly created for RBG strips
void childSetColorRGBStrip(String dni, value) {
    def name = dni.split("-")[-1]
    log.debug "childSetColorRGBStrip($dni), name = ${name}, colorRGB = ${value}"
    sendEthernet("${name} ${value}")
}

void childSetColorRGBW(String dni, value) {
    def name = dni.split("-")[-1]
    log.debug "childSetColorRGBW($dni), name = ${name}, colorRGBW = ${value}"
    sendEthernet("${name} ${value}")
}

void childRelayOn(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childRelayOn($dni), name = ${name}"
    sendEthernet("${name} on")
}

void childRelayOff(String dni) {
    def name = dni.split("-")[-1]
    log.debug "childRelayOff($dni), name = ${name}"
    sendEthernet("${name} off")
}

def configure() {
	log.debug "Executing 'configure()'"
    updateDeviceNetworkID()
	sendEvent(name: "numberOfButtons", value: numButtons)
}

def refresh() {
	log.debug "Executing 'refresh()'"
	sendEthernet("refresh")
	sendEvent(name: "numberOfButtons", value: numButtons)
}

def installed() {
	log.debug "Executing 'installed()'"
    if ( device.deviceNetworkId =~ /^[A-Z0-9]{12}$/)
    {
    }
    else
    {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")
    }
}

def initialize() {
	log.debug "Executing 'initialize()'"
    sendEvent(name: "numberOfButtons", value: numButtons)
}
def updated() {
	if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 5000) {
		state.updatedLastRanAt = now()
		log.debug "Executing 'updated()'"
    	runIn(3, "updateDeviceNetworkID")
		sendEvent(name: "numberOfButtons", value: numButtons)
        log.debug "Hub IP Address = ${device.hub.getDataValue("localIP")}"
        log.debug "Hub Port = ${device.hub.getDataValue("localSrvPortTCP")}"
	}
	else {
//		log.trace "updated(): Ran within last 5 seconds so aborting."
	}
}


def updateDeviceNetworkID() {
	log.debug "Executing 'updateDeviceNetworkID'"
    if(device.deviceNetworkId!=mac) {
    	log.debug "setting deviceNetworkID = ${mac}"
        device.setDeviceNetworkId("${mac}")
	}
    //Need deviceNetworkID updated BEFORE we can create Child Devices
	//Have the Arduino send an updated value for every device attached.  This will auto-created child devices!
	refresh()
}

private void createChildDevice(String deviceName, String deviceNumber) {
    if ( device.deviceNetworkId =~ /^[A-Z0-9]{12}$/) {
    
		log.trace "createChildDevice:  Creating Child Device '${device.displayName} (${deviceName}${deviceNumber})'"
        
		try {
        	def deviceHandlerName = ""
        	switch (deviceName) {
         		case "contact": 
                		deviceHandlerName = "Child Contact Sensor" 
                	break
         		case "switch": 
                		deviceHandlerName = "Child Switch" 
                	break
         		case "dimmerSwitch": 
                		deviceHandlerName = "Child Dimmer Switch" 
                	break
         		case "rgbSwitch": 
                		deviceHandlerName = "Child RGB Switch" 
                	break
                case "rgbStrip": 
                		deviceHandlerName = "Child RGB Strip" 
                	break
         		case "generic": 
                		deviceHandlerName = "Child Generic Sensor" 
                	break
         		case "rgbwSwitch": 
                		deviceHandlerName = "Child RGBW Switch" 
                	break
         		case "relaySwitch": 
                		deviceHandlerName = "Child Relay Switch" 
                	break
         		case "temperature": 
                		deviceHandlerName = "Child Temperature Sensor" 
                	break
         		case "humidity": 
                		deviceHandlerName = "Child Humidity Sensor" 
                	break
         		case "motion": 
                		deviceHandlerName = "Child Motion Sensor" 
                	break
         		case "water": 
                		deviceHandlerName = "Child Water Sensor" 
                	break
         		case "illuminance": 
                		deviceHandlerName = "Child Illuminance Sensor" 
                	break
         		case "illuminancergb": 
                		deviceHandlerName = "Child IlluminanceRGB Sensor" 
                	break
         		case "voltage": 
                		deviceHandlerName = "Child Voltage Sensor" 
                	break
         		case "smoke": 
                		deviceHandlerName = "Child Smoke Detector" 
                	break    
         		case "carbonMonoxide": 
                		deviceHandlerName = "Child Carbon Monoxide Detector" 
                	break    
         		case "alarm": 
                		deviceHandlerName = "Child Alarm" 
                	break    
         		case "doorControl": 
                		deviceHandlerName = "Child Door Control" 
                	break
         		case "ultrasonic": 
                		deviceHandlerName = "Child Ultrasonic Sensor" 
                	break
         		case "presence": 
                		deviceHandlerName = "Child Presence Sensor" 
                	break
         		case "power": 
                		deviceHandlerName = "Child Power Meter" 
                	break
			default: 
                		log.error "No Child Device Handler case for ${deviceName}"
      		}
            if (deviceHandlerName != "") {
         		addChildDevice(deviceHandlerName, "${device.deviceNetworkId}-${deviceName}${deviceNumber}", null,
         			[completedSetup: true, label: "${device.displayName} (${deviceName}${deviceNumber})", 
                	isComponent: false, componentName: "${deviceName}${deviceNumber}", componentLabel: "${deviceName} ${deviceNumber}"])
        	}   
    	} catch (e) {
        	log.error "Child device creation failed with error = ${e}"
        	state.alertMessage = "Child device creation failed. Please make sure that the '${deviceHandlerName}' is installed and published."
	    	runIn(2, "sendAlert")
    	}
	} else 
    {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")
    }
}

private sendAlert() {
   sendEvent(
      descriptionText: state.alertMessage,
	  eventType: "ALERT",
	  name: "childDeviceCreation",
	  value: "failed",
	  displayed: true,
   )
}

private boolean containsDigit(String s) {
    boolean containsDigit = false;

    if (s != null && !s.isEmpty()) {
//		log.debug "containsDigit .matches = ${s.matches(".*\\d+.*")}"
		containsDigit = s.matches(".*\\d+.*")
    }
    return containsDigit
}