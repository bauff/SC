/* iz Mon 08 October 2012  4:48 PM EEST
Code management + performance tool.
Read/Display/Edit/Interact/Store code organized in files holding snippets of code.

The code is organized in a tree with 3 levels:

1. Folders
3. Files
4. Snippets (Self-sufficient Units of code)

The code is stored in a single file as SC text archive. It can also be exported as a hierarchy of folders and files, and reimported from files and folders.

- If importing a folder of folders, the folders of the top folder are added to the existing folder list
- Names of items in the list are created from the name of the file or folder.
- If there is a conflict (the new file or folder imported has the same name as an existing element),
  then the name of the newly imported item is changed

- The entire library instance is auto-saved every time that its window is closed or supercollider
  shuts down / re-compiles.
  If no path is defined for saving the data, then a file save dialog opens.

Menus:

- File Menu
- Folders
- Files
- Snippets

*/

ScriptLibGui : AppModel {
	classvar <>font, <windowShift = 0;
	var <scriptLib;
	var <snippetViews;

	*initClass {
		StartUp add: {
			font = Font.default.size_(10);
		}
	}

	gui {
		this.stickyWindow(scriptLib, windowInitFunc: { | window |
			window.name = scriptLib.path ? "ScriptLib";
			window.bounds = Rect(
				windowShift + 500,
				windowShift.neg + 350, 800, 500);
			windowShift = windowShift + 20 % 200;
			window.layout = VLayout(
				this.topMenuRow,
				this.itemEditor.hLayout(font),
				HLayout(
					this.listView('Folder').dict(scriptLib.lib).view.font = font,
					this.listView('File').branchOf('Folder').view.font = font,
					this.listView('Snippet').branchOf('File', { | adapter, name |
						format("//:%\n{ WhiteNoise.ar(0.1) }", name)
					})
					.view.font_(font).keyDownAction_({ | view, char, mod, ascii |
						switch (ascii,
							27, { this.proxySelectWindow },
							13, { this.evalSnippet(mod) }, // return key,
							32, { this.toggleProxy }, // space key
						);
					}),
				),
				this.snippetButtonRow,
				[this.snippetCodeList, s: 3]
			);
			this.windowClosed(window, {
				scriptLib.save;
				this.objectClosed;
			})
		});
	}

	topMenuRow {
		^HLayout(
			this.popUpMenu(\topMenu,
			{ ["File Menu", "New", "Open", "Save", "Save as", "Import", "Export"] }
			).view.font_(font).action_({ | me |
				this.mainMenuAction(me.value);
				me.value = 0
			}),
			this.itemEditMenu('Folder')
			.view.font_(font),
			this.itemEditMenu('File')
			.view.font_(font),
			this.itemEditMenu('Snippet')
			.view.font_(font),
		);
	}

	mainMenuAction { | actionIndex = 0 |
		[nil,	// MainMenu item. Just header. No action.
		{ ScriptLib.new.addDefaults.gui }, 		// New
		{ ScriptLib.open; },		// Open
		{ scriptLib.save; },			// Save
		{ scriptLib.saveDialog },		// Save as
		{ Dialog.openPanel({ | path | scriptLib.import(path) }) }, // Import
		{ Dialog.savePanel({ | path | scriptLib.export(path) }) }, // Export
		][actionIndex].value;
	}

	snippetButtonRow {
		^HLayout(
			Button().font_(font).states_([["list"], ["edit"]]).action_({ | me |
				snippetViews.index = me.value
			}),
			this.button('Snippet').action_({ | me |
				me.getString.interpret.postln;
			}).view.font_(font).states_([["run", Color.red]]),
			this.button('Proxy')
			.proxyWatcher({ | me |
				me.checkProxy(me.value.adapter.item.checkEvalPlay(this.getValue('Snippet').getString))
			})
			.view.font_(font).states_([[">", nil, Color.green], ["||", nil, Color.red]]).fixedWidth_(30),
			this.button('Snippet').action_({ | me |
				this.getValue(\Proxy).item.evalSnippet(me.getString, start: false, addToSourceHistory: false);
			}).view.font_(font).states_([["set proxy source"]]),
			this.popUpMenu('Proxy').proxyList(ProxyCentral.default.proxySpace)
			.view.fixedWidth_(30).font_(font).background_(Color.yellow),
			this.button('Snippet').action_({ | me |
				scriptLib.addSnippetNamed(*(me.value.adapter.path ++ [me.value.item, me.getString]));
				me.getString.postln;
				"=============== SNIPPET SAVED ===============".postln;
				// Following can be removed when SC3.6 stops crashing at recompile with ScriptLibGui open.
				scriptLib.save;
			}).view.font_(font).states_([["save"]]),
			this.button('Snippet').action_({ | me |
				scriptLib.addSnippet(*(me.value.adapter.path ++ [me.getString, true]));
			}).view.font_(font).states_([["new"]]),
			this.button('Snippet').action_({ | me |
				scriptLib.deleteSnippet(*(me.value.adapter.path ++ [me.item]));
			}).view.font_(font).states_([["delete"]]),
			Button().states_([["mixer"]]).action_({ ScriptMixer.activeMixer }).font_(font),
			this.button('Snippet').action_({ | me |
				ProxyCodeEditor(ProxyCentral.default.proxySpace, ProxyCentral.currentProxy);
			}).view.font_(font).states_([["proxy editor"]]),
			this.button('Snippet').action_({ | me |
				SoundFileGui();
			}).view.font_(font).states_([["samples"]]),
		)
	}

	snippetCodeList {
		^snippetViews = StackLayout(
			this.textView('Snippet').listItem({ | me |
				me.value.adapter.dict.atPath(me.value.adapter.path ++ [me.item])
			}).makeStringGetter.view.font_(Font("Monaco", 10)).tabWidth_(25),
			this.snippetListView
		)
	}

	snippetListView {
		var listView, proxyIndex, colors;
		colors = [Color(0.95, 0.95, 0.96), Color(1, 1, 0.99)].dup(30).flat;
		listView = this.listView('Snippet', { | me |
			var snippets;
			snippets = me.value.adapter.dict.atPath(me.value.adapter.path);
			// Must do this here, because resetting the items of the list also resets the colors:
			{ me.view.colors = colors }.defer(0.03);
			if (snippets.isNil) { [] } { snippets.asSortedArray.flop[1] };
		}).view.font_(Font("Monaco", 10));
		listView.keyDownAction = { | view, char, mod, ascii |
			switch (ascii,
				13, { this.evalSnippet(mod) }, // return key,
				32, { this.toggleProxy }, // space key
				{
					proxyIndex = "12345678qwertyuiasdfghjkzxcvbnm," indexOf: char;
					proxyIndex !? { this.getValue('Proxy').index_(nil, proxyIndex) }
				}
			);
		};
		^listView;
	}

	proxySelectWindow {
		var w;
		w = Window(bounds: Rect(400, 100, 100, 600)).front;
		w.layout = VLayout(ListView(w)
			.items_(Array.new.addAll("12345678qwertyuiasdfghjkzxcvbnm,"))
			.keyDownAction_({ | me, char, mod, ascii |
				if (ascii == 13) { w.close }
			})
			.action_({ | me | this.getValue('Proxy').index_(nil, me.value) })
		)
	}

	evalSnippet { | mod = 0 |
		if (mod == 0) {
			this.getValue(\Proxy).item.evalSnippet(
				this.getValue('Snippet').getString, start: false, addToSourceHistory: false
			);
		}{
			this.getValue('Snippet').getString.postln.interpret;
		}
	}

	toggleProxy {
		this.getValue('Proxy').changed(\toggle);
	}
}



