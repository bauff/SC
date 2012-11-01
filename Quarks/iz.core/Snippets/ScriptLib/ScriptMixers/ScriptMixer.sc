/* IZ Thu 16 August 2012  6:28 PM EEST

Uses default ProxySpace from ProxyCentral.

12345678
qwertyui
asdfghjk
zxcvbnm,

ScriptMixer.activeMixer;

ScriptMixer();

*/

ScriptMixer : AppModel {
	classvar activeMixer;		// instance of mixer whose window is foremost
	var <>numStrips = 8, <numPresets = 8, <proxySpace, <strips, <proxyList;
	var <stripWidth = 80;
	var <>font;
	var <valueCache;	// fast access to values for storing and restoring presets

	*initClass {
		Class.initClassTree(MIDISpecs);
		MIDISpecs.put(this, this.uc33eSpecs);
	}

	*activeMixer {
		activeMixer ?? { activeMixer = this.new };
		^activeMixer;
	}

	*new { | numStrips = 8, numPresets = 8 | ^super.new(numStrips, numPresets).init; }

	init {
		font = Font.default.size_(9);
		proxySpace = ProxyCentral.default.proxySpace;
		this.makeStrips;
		this.makeWindow;
//		this.initPresets;
		this.reloadProxies;
		this.initMIDI;
	}

	*evalSnippet { | snippetString, start = false |

	}

	makeStrips {
		strips = { | index | ScriptMixerStrip(this, index) } ! numStrips;
	}

	reloadProxies { proxyList.changed(\list, proxyList); }

	makeWindow {
		var winWidth;
		winWidth = stripWidth * numStrips;
		this.window({ | w, app |
			w	.name_("Proxy Script Mixer")
				.bounds_(Rect(Window.screenBounds.width - winWidth, 0, winWidth, 250))
				.layout = HLayout(
/*					VLayout(
						*(this.radioButtons(
							\presets,
							{ "just a placeholder" } ! numPresets,
							{ | me | this.setPreset(me.item); },
							{ | me | this.getPreset(me.item); },
						) collect: _.font_(font))
					),
*/
				*(strips collect: _.gui)
				);
			this.addWindowActions(w);
		});
	}

	addWindowActions { | window |
		this.windowClosed(window, {
			this.makeInactive;
			this.disable(window);
			this.objectClosed;
		});
		this.windowToFront(window, { this.enable; this.makeActive; });
		this.windowEndFront(window, { this.disable; });
		window.addNotifier(this, \colorEnabled, {
			if (window.isClosed.not) { window.view.background = Color(*[0.7, 0.8, 0.9]) }
		});
		window.addNotifier(this, \colorDisabled, {
			if (window.isClosed.not) { window.view.background = Color(0.8, 0.8, 0.8, 0.05); };
		});
	}

	enable {
		super.enable(true);
		strips do: _.enable;
		this.changed(\colorEnabled);
//		activeMixer = this;
	}

	disable {
		super.disable;
		strips do: _.disable;
		this.changed(\colorDisabled);
//		if (activeMixer === this) { activeMixer = nil };
	}

	makeActive { activeMixer = this }
	makeInactive { if (activeMixer === this) { activeMixer = nil }; }


	// PRESETS
/*
	initPresets {
		var protoPreset;
		valueCache = strips.collect(_.valueCache).flat;
		protoPreset = valueCache collect: _.item;
		this.getValue(\presets).items_(nil, { List.newUsing(protoPreset) } ! numPresets);
	}

	getPreset { | preset | preset.array = valueCache collect: _.item; }

	setPreset { | preset |
		valueCache do: { | v, i | v.item_(nil, preset[i]) };
	}
*/
	// MIDI

	initMIDI {
		var specs, knob, slider, strip;
		specs = this.midiSpecs;
		knob = specs[\knob];
		slider = specs[\slider];
		// _mean_ shortcut creating midi specs by setting the channel number for each strip:
		8.min(numStrips) do: { | i |
			strips[i].addMIDI([slider: slider.put(3, i), knob: knob.put(3, i)]);
		}
	}

	*uc33eSpecs { // these specs are for M-Audio U-Control UC-33e, 1st program setting
		^(
			knob: [\cc, nil, 10, 0],
			slider: [\cc, nil, 7, 0]
/*			startStopButton: [\cc, { | me | me.toggle }, 18, 0],
			prevSnippet: [\cc, { | me | me.action.value }, 19, 0],
			eval: [\cc, { | me | me.action.value }, 20, 0],
			nextSnippet: [\cc, { | me | me.action.value }, 21, 0],
			firstSnippet: [\cc, { | me | me.action.value }, 22, 0],
			add: [\cc, { | me | me.action.value }, 23, 0],
			lastSnippet: [\cc, { | me | me.action.value }, 24, 0],
			resetSpecs: [\cc,  { | me | me.action.value }, 25, 0],
			toggleWindowSize: [\cc,  { | me | me.toggle }, 26, 0],
			delete: [\cc,  { | me | me.action.value }, 27, 0],
*/		)
	}

}

