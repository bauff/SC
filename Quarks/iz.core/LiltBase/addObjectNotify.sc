
/* 
Shortcuts for establishing messaging communication between objects via NotificationCenter.
*/

+ Object {
	notify { | message, args | NotificationCenter.notify(this, message, args); }
	
	addMessage { | notifier, message |
		NotificationCenter.register(notifier, message, this, { | ... args | 
			this.performList(message, args)
		});
	}

	removeMessage { | notifier, message |
		NotificationCenter.unregister(notifier, message, this);
	}

	addSelfNotifier { | notifier, message, action |
		// for widgets that need to use themselves in the action to set their value
		this.addNotifier(notifier, message, WidgetAction(this, action));
	}
	
	addNotifier { | notifier, message, action |
	// add self to do action when receiving message from notifier
	// if either object (notifier or self) closes, remove the notifier->receiver connection
		NotificationCenter.register(notifier, message, this, action);
		this onObjectClosed: { NotificationCenter.unregister(notifier, message, this); };
		notifier onObjectClosed: { NotificationCenter.unregister(notifier, message, this); };
	}

	removeNotifier { | notifier, message |
		// leaves the onClose connection dangling, 
		// which will be removed when the object calls objectClosed
		 NotificationCenter.unregister(notifier, message, this);
	}

	addListener { | listener, message, action |
	// add listener to do action when receiving message from self
	// if either object (listener or self) closes, remove the notication connection
		NotificationCenter.register(this, message, listener, action);
		this onClose: { NotificationCenter.unregister(this, message, listener); };
		listener onClose: { NotificationCenter.unregister(this, message, listener); }
	}

	removeListener { | listener, message |
		 NotificationCenter.unregister(this, message, listener);
	}
	
	objectClosed {	// remove all notifiers and listeners // and inputs from widgets
		NotificationCenter.notify(this, this.removedMessage);
		// is the next one too cpu costly to do every time an object closes? 
//		this.disable;	// free MIDI or osc inputs from all widgets
		this.removeAllNotifications;
		Widget.removeModel(this);
	}
	
	removeAllNotifications {
		NotificationCenter.removeAll(this);
	}

	removedMessage { ^\objectClosed }

	onObjectClosed { | action | this.onRemove(UniqueID.next, action) } 
	onRemove { | key, func | this.doOnceOn(this.removedMessage, key, func); }
	doOnceOn { | message, receiver, func |
		this.registerOneShot(message, receiver, { func.(this) });
	}
	registerOneShot { | message, receiver, func |
		NotificationCenter.registerOneShot(this, message, receiver, func);
	}

	addToServerTree { | function, server |
		ServerPrep(server).addToServerTree(this, function);
	}
	removeFromServerTree { | function, server |
		ServerPrep(server).removeFromServerTree(this);
	}
}

+ NotificationCenter {
	*removeAll { | object |
		registrations.removeEmptyAtPath([object]);
	}	
}