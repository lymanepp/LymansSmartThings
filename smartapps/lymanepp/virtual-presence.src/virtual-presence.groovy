/**
 *  Virtual Presence
 *
 *  Copyright 2017 Lyman Epp
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
 *  Author: Lyman Epp, Based on work by fuzzysb with thanks
 */
definition(
	name: "Virtual Presence",
	namespace: "lymanepp",
	author: "Lyman Epp",
	description: "Triggers Virtual Presence when HTTP GET Request is received",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section(title: "Select Devices") {
		input "virtualPresence", "capability.presenceSensor", title: "Virtual presence sensors", required: true, multiple: true
		input "anyPresence", "capability.presenceSensor", title: "Presence sensor if anyone is present (optional)", required: false, multiple: false
		input "everyPresence", "capability.presenceSensor", title: "Presence sensor if everyone is present (optional)", required: false, multiple: false
	}
}

def installed() {
	init()
	createAccessToken()
	getToken()
	log.debug "Installed Virtual Presence with app id: $app.id"
	log.debug "Installed Virtual Presence with token: $state.accessToken"
}

def updated() {
	unsubscribe()
	init()
	log.debug "Updated Virtual Presence with app id: $app.id"
	log.debug "Updated Virtual Presence with token: $state.accessToken"
}

def init() {
    state.departed = []
}

mappings {
  path("/presence/:sensor/:status") {
	action: [
	  GET: "updatePresence"
	]
  }
}

def updatePresence() {
	log.debug "Presence $params.sensor $params.status"

	def sensor = virtualPresence.find { params.sensor.equalsIgnoreCase(it.name) }
    if (sensor) {
        setPresence(sensor, params.status.equalsIgnoreCase("present"));
	}
    
	if (anyPresence) {
        Boolean present = virtualPresence.any { it.currentValue("presence").equalsIgnoreCase("present") }
		setPresence(anyPresence, present);
	}
	
	if (everyPresence) {
        Boolean present = virtualPresence.every { it.currentValue("presence").equalsIgnoreCase("present") }
		setPresence(everyPresence, present);
	}
}

def setPresence(sensor, present) {
    if (present) {
        if (state.departed.remove(sensor.name) && state.departed.size() == 0) {
        	unschedule(departedHandler)
        }
        
        if (!sensor.currentValue("presence").equalsIgnoreCase("present")) {
	        log.debug "Executing arrived for $sensor.name"
        	sensor.arrived()
        }
    } else {
        log.debug "Scheduling departed for $sensor.name"
        state.departed.add(sensor.name)
        runIn(600, departedHandler)
    }
}

def departedHandler() {
	state.departed.each {
    	def sensor = getPresenceSensor(it)
	    if (sensor.currentValue("presence").equalsIgnoreCase("present")) {
            log.debug "Executing departed for $sensor.name"
            sensor.departed()
        }
    }
    state.departed = []
}

def getPresenceSensor(name) {
	def sensor = virtualPresence.find { it.name == name }
    if (sensor)
        return sensor
    
	if (anyPresence && anyPresence.name == name)
    	return anyPresence
	
	if (everyPresence && everyPresence.name == name)
    	return everyPresence
    
    return null
}

def getToken() {
	if (!state.accessToken) {
		try {
			getAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.debug "Did you enable OAuth in SmartApp IDE settings?"
			log.debug ex
		}
	}
}