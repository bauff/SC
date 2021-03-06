/* iz Thu 04 October 2012  5:33 PM EEST

Keep a history of n recent paths for an object. Create a dialog pane and dialog panels for opening / saving an objects data in one of the paths chosen from the

The history is stored in the default archive (Platform.userAppSupportDir +/+ "archive.sctxar"), in a dictionary under a symbol which is the objectID.

The objectID is used to identify the object that is using the path history, keeping the id immutable between compile sessions. The simplest case is to use the Class itself as ID and let RecentPaths turn the class name into a symbol which server as objectID.

See also: Prefs.

RecentPaths(\test).openDialog({ | thePath | ["opening", thePath].postln; }, { | thePath | ["creating", thePath].postln; });

Example using RecentPaths: ScriptLib;

RecentPaths are not stored in the global archive but in a separate file, because they would be lost if the library is loaded without the RecentPaths class installed.

TODO: Add delete button.

*/

RecentPaths {
	classvar <all; // all RecentPaths instances, by objectID
	// root path at Library for objects stored by their paths:
	classvar <libPath = 'InstancePaths';

	var <objectID, <>numHistoryItems = 20, <paths, <default;

	// Find or create an instance holding the recent paths for objectID
	*new { | objectID, numHistoryItems = 20 |
		var instance;
		objectID = objectID.asSymbol;
		all ?? { all = Object.readArchive(this.archiveFilePath); };
		all ?? { all = IdentityDictionary(); };
		instance = all[objectID];
		instance ?? {
			instance = this.newCopyArgs(objectID, numHistoryItems).init;
			all[objectID] = instance;
			this.saveAll;
		};
		^instance;
	}

	*allObjects { ^Library.at(this.libPath) }

	*saveAll { all.writeArchive(this.archiveFilePath);  /* "Recent paths saved".postln; */ }
	*archiveFilePath { ^Platform.userAppSupportDir +/+ "RecentPaths.sctxar" }

	*openDefault { | objectID, openAction, createAction |
		var recentPaths, path, instance;
		recentPaths = this.new(objectID.asSymbol);
		path = recentPaths.default;
		if (path.isNil) {
			recentPaths.openDialog(openAction, createAction);
		}{
			recentPaths.selectExistingOrOpen(path, openAction);
		}
	}

	selectExistingOrOpen { | path, openAction |
		var existing;
		existing = this.getInstanceAtPath(path);
//		[thisMethod.name, "existing is", existing].postln;
		if (existing.notNil) { ^this.opened(existing) };
		^this.addInstanceAtPath(path, this.opened(openAction.(path)));
	}

	opened { | instance |
		instance.changed(\opened, instance);
		^instance;
	}

	getInstanceAtPath { | path |
		^Library.at(libPath, objectID, path.asSymbol);
	}

	addInstanceAtPath { | path, instance |
		var previousPath, pathSymbol, pathString;
		pathSymbol = path.asSymbol;
		pathString = path.asString;
		instance.changed(\path, path);
		// retrospective correction of already saved instances from older version:
		if (paths.isKindOf(List).not) { paths = List.newUsing(paths); }; // safe!
		paths.remove(paths detect: { | p | p == pathString });
		paths add: pathString;
		if (paths.size > numHistoryItems) { paths.pop };
		previousPath = this.getPathFor(instance);
		previousPath !? { this.deleteAtPath(pathSymbol); };
		this.addAtPath(pathSymbol, instance);
		this.addNotifier(instance, \objectClosed, {
			this.deleteAtPath(pathSymbol); postf("% closed\n", instance);
		});
		this.class.saveAll;
		^instance;
	}

	addAtPath { | path, object |
		Library.put(libPath, objectID, path.asSymbol, object);
	}
	deleteAtPath { | path |
		Library.put(libPath, objectID, path.asSymbol, nil);
	}

	*getInstanceAtPath { | objectID, path |
		^this.new(objectID.asSymbol).getInstanceAtPath(path.asSymbol)
	}

	*defaultPathFor { | objectID |
		^this.new(objectID.asSymbol).default;
	}

	*openDialog { | objectID, openAction, createAction |
		^this.new(objectID).openDialog(openAction, createAction);
	}

	*save { | objectID, action, object |
		var path;
		path = this.getPathFor(objectID, object);
		if (path.isNil) {
			this.savePanel(objectID, action, object)
		}{
			this.saveToPath(objectID, action, object, path)
		}
	}

	getPathFor { | object |
		^this.class.getPathFor(objectID, object)
	}

	*getPathFor { | objectID, object |
		var dict;
		dict = Library.at(libPath, objectID);
		if (dict.isNil) { ^nil } { ^dict.findKeyForValue(object); };
	}

	*savePanel { | objectID, action, object |
		^this.new(objectID.asSymbol).savePanel(action, object);
	}

	savePanel { | action, object |
		Dialog.savePanel({ | path | this.saveToPath(action, object, path); });
	}

	*saveToPath { | objectID, action, object, path |
		this.new(objectID).saveToPath(action, object, path)
	}

	saveToPath { | action, object, path |
		action.(path);
		this.addInstanceAtPath(path, object);
	}

	init { paths = List.new; }

	default_ { | argDefault |
		default = argDefault;
		postf("Setting default path for: %\nPath is: %\n", objectID, argDefault);
		this.class.saveAll;
	}

	openDialog { | openAction, createAction |
		var buttons;
		buttons add: StaticText().string_("... or choose a path from the recent paths below:");
		AppModel().window({ | window, app |
			window.bounds = window.bounds.left_(250).width_(800);
			window.name = format("Select path for: %", objectID);
			buttons = Array(3);
			// Button for creating new instance, without selecting any path
			createAction !? {
				buttons add: Button().states_([["Create new"]])
				.action_({ this.opened(createAction.(this)); window.close; });
			};
			// Button for loading an instance from path selected by user via open panel dialog
			buttons add: Button().states_([["Open from disc ... "]]).action_({
				this.openPanel(openAction, window);
			});
			buttons add: StaticText().string_("... or select from list below:");
			window.layout = VLayout(
				HLayout(*buttons),
				// List of recent paths.
				app.listView(\paths).items_(paths).view,
				HLayout(
					// Button for loading instance from path selected from recent paths list
					app.button(\paths).action_({ | me |
//						["select existing or open with item", me.item].postln;
						this.selectExistingOrOpen(me.item, openAction);
						window.close;
					}).view.states_([["Accept"]]),
					app.button(\paths).action_({ window.close }).view.states_([["Cancel"]]),
				)
			)
		})
	}

	openPanel { | action, window |
		Dialog.openPanel({ | path |
			this.selectExistingOrOpen(path, action);
			window.close;
		})
	}
}
