/* IZ Wed 29 August 2012 12:57 PM EEST

BufferItems are accessible by the name of the file from which they were loaded, without extension, as a symbol, sending it message 'b'.

Once a BufferItem is loaded, it will reload when the default server reboots.

// To access or play a loaded buffer item:

\SinedPink.b; // accesses the buffer

\SinedPink.b.play // accesses and plays the buffer

BufferItems can be added, loaded, deleted, free'd through the BufferListGui.

BufferListGui();

*/

BufferItem : NamedItem {
	// name -> path. item -> Buffer
	// Buffer allocated only and always when server boots or is booted.
	classvar loadingBuffers; // Used to load buffers only one at a time. See method load.
	classvar <>all;	// IdentityDictionary with one buffer per symbol.
					// prevent creating duplicate buffers with same path.
	classvar <loadedBuffersPath = 'Buffers';
	var <>nameSymbol;

	*getBuffer { | bufferName |
		^this.loadedBuffers[bufferName.asSymbol];
	}

	*loadedBuffers { ^Library.at(loadedBuffersPath) }

	*initClass {
		loadingBuffers = IdentityDictionary.new;
		all = IdentityDictionary.new;
		StartUp add: {
			Library.put(loadedBuffersPath, IdentityDictionary.new);
			ServerBoot.add({
				Library.at(loadedBuffersPath) do: _.loadIfNeeded;
			}, Server.default);
//			ServerQuit.add({
//				Library.at('Buffers') do: _.serverQuit;
//			}, Server.default);
		}
	}

	*new { | name |
		var nameSymbol, existing;
		nameSymbol = PathName(name).fileNameWithoutExtension.asSymbol;
		(existing = all[nameSymbol]) !? { ^existing };
		^super.new(name).nameSymbol_(nameSymbol).register;
	}

	*named { | name | ^all[name.asSymbol] }

	*free { | bufferName |
		var bufferItem;
		(bufferItem = this.loadedBuffers[bufferName.asSymbol]) !? { bufferItem.free };
	}

	register { all[nameSymbol] = this }

	rebuild {
		var existing;
		item = nil;
		existing = all[nameSymbol];
		if (existing.notNil) {
			^existing;
		}{
			all[nameSymbol] = this;
			^this;  // (;-)
		}
	}

	loadIfNeeded { | fileNotFoundAction |
		if (item.isNil) {
			this.load(nil, fileNotFoundAction);
		}{
			item.updateInfo({ | buffer |
				if (buffer.numChannels == 0) {
					this.load(nil, fileNotFoundAction);
				}{
					postf("Buffer '%' already loaded. Skipping.\n", nameSymbol);
				}
			});
		}
	}

	load { | extraAction, fileNotFoundAction | // mechanism for loading next buffer after this one is loaded
		if (File.exists(name.asString).not) {
			postf("Cannot load BufferItem. File not found!\n%\n", name);
			^this
		};
		if (Server.default.serverRunning) {
			loadingBuffers[this] = { this.prLoad(extraAction, fileNotFoundAction); };
			if (loadingBuffers.size == 1) { this.prLoad(extraAction, fileNotFoundAction); }
		}{
			this.storeInLibrary;
			if (Server.default.serverBooting.not) { Server.default.boot };
		};
	}

	prLoad { | extraAction, fileNotFoundAction |
		// called from loadingBuffers when previous buffer is loaded
		var action;
		action = { | buffer |
			item = buffer;
			buffer.sampleRate = buffer.sampleRate ? 44100;
			this.postInfo;
			this.storeInLibrary;
			extraAction.(buffer);
			loadingBuffers[this] = nil;
			loadingBuffers.detect(true).value;
		};
		if (File.exists(name)) {
			Buffer.read(Server.default, name, action: action)
		}{
			postf("BufferItem: File not found!\n%\nAllocating empty buffer\n", name);
			Buffer.alloc(Server.default, 4096, 1, action);
			[this, thisMethod.name, "fileNotFoundAction is:", fileNotFoundAction].postln;
			fileNotFoundAction.(this);
		}
	}

/*
	serverQuit {
		// Restore the buffer if the server has not really quit:
		item.updateInfo({ | buffer | item = buffer });
		item = nil;
	}
*/

	play {
		item !? { ^item.play };
		this.load({ item.play })
	}

	postInfo { postf("% : % \n", this.minSec, nameSymbol) }

	minSec {
		var seconds;
		item ?? { ^"?? min, ?? sec" };
		^minSec(item.numFrames / item.sampleRate);
	}

	free {
		var registeredItem;
		registeredItem = Library.at(loadedBuffersPath, nameSymbol);
		registeredItem !? { if (registeredItem !== this) { ^registeredItem.free }; };
		item !? { item.free; };
		item = nil;
		Library.put(loadedBuffersPath, this.nameSymbol, nil);
		this.updateLists;
	}

	updateLists { this.class.updateLists; }

	*updateLists {
		var buffers;
		(buffers = Library.at(loadedBuffersPath)) !? {
			{ this.changed(\bufferList, Library.at(loadedBuffersPath).keys.asArray.sort); }.defer;
		}
	}

	storeInLibrary {
		Library.put(loadedBuffersPath, this.nameSymbol, this);
		this.updateLists;
	}

	*openPanel { | doneFunc |
		Dialog.openPanel({ | path | doneFunc.(this.new(path)); });
	}

	*makeLoadBuffersString {
		var buffers;
		buffers = Library.at(loadedBuffersPath).asArray;
		if (buffers.size == 0) { ^"" };
		^buffers.inject("\n// ====== BUFFERS ====== \n\n", { | str, b |
			str ++ format("BufferItem(%).load;\n", b.name.asCompileString);
		});
	}

}
