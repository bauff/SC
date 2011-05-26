/* A Resource for receiving and processing TouchOSC input */

TouchOSC : Resource {
	var <responder;
	var <sessionDict;		// IdentityDictionary with all alive sessions by ID
	var <>verbose = false;
	// Note: The array of current session IDs is stored in object. 

	init {
		this.makeResponder;
		sessionDict = IdentityDictionary.new;
		object = [];
	}

	
	makeResponder {
		responder	= OSCresponder(nil, this.oscMessage, { | time, addr, msg |
			this.perform(msg[1], msg[2..]);
		}).add;
	}
	
	oscMessage { ^'/tuio/2Dcur' }

	// ====== set message: check which sessions were born or moved =====
	set { | data |
		var session; 		// session object, if already alive.
		var sessionID;
		sessionID = data[0];
		session = sessionDict[sessionID];
		if (session.isNil) {
			this.sessionStarted(sessionID, data);
		}{
			this.sessionChanged(session, data);
		}
	}

	sessionStarted { | sessionID, data |
		sessionDict[sessionID] = this.sessionClass.new(this, sessionID, data);
	}

	sessionClass { ^TouchSession }

	sessionChanged { | session, data |
		session.sessionChanged(data);
	}

	// ====== alive message: check which sessions have ended =====
	alive { | activeSessionIDs |
		/* here we compare the activeSessionIDs received with those of the previous frame.
		If any IDs of the previous frame are not present in this frame, then these have died. */
		object do: this.findOutIfSessionStillAlive(_, activeSessionIDs);
	}
	
	findOutIfSessionStillAlive { | sessionID, activeSessionIDs |
		if ((object includes: sessionID).not) {
			this.sessionEnded(sessionID);
		}
	}
	
	sessionEnded { | sessionID |
		var session;
		session = sessionDict[sessionID];
		session.sessionEnded;
		sessionDict.removeAt(sessionID);	
	}
		
	fseq { | frameID |
		/* we dont need to do something at this stage, just post some useful data */
		if (verbose) { 
			postf("% frame: %, sessions alive: %\n", this.class.name, frameID, object);
		}
	}	
}

FiducialOSC : TouchOSC {
	
	oscMessage { ^'/tuio/2Dobj' }

	sessionClass { ^FiducialSession }

}