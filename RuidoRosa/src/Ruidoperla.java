
//import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;

import ch.bildspur.postfx.builder.PostFX;
import controlP5.ControlP5;
import controlP5.Println;
import de.looksgood.ani.Ani;
import de.looksgood.ani.AniSequence;
import micycle.peasygradients.PeasyGradients;
import micycle.peasygradients.gradient.Gradient;
//import micycle.peasygradients.gradient.Palette;
import micycle.peasygradients.gradient.*;

import micycle.peasygradients.utilities.Interpolation;
import peasy.CameraState;
import peasy.PeasyCam;
import peasy.org.apache.commons.math.geometry.Rotation;
import peasy.org.apache.commons.math.geometry.RotationOrder;
import peasy.org.apache.commons.math.geometry.Vector3D;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PVector;
//import processing
import processing.data.JSONArray;
import processing.data.JSONObject;
//import processing.opengl.*;

import processing.sound.*;

//ARDUINO SEriaL
import processing.serial.*;

public class Ruidoperla extends PApplet {

	public static void main(String[] args) {
		PApplet.main("Ruidoperla");
	}

	// SETTINGS

	public static final int WIDTH = 1280;
	public static final int HEIGHT = 720;
	private static final float SPEECH_TIME = 5;
	public static final int BEAT_SENTIVITY = 1;
	public static final boolean TEXT2SPEECH = false;

	boolean CAM_ANIM = true;
	boolean GEOMETRY_ANIM = true;
	int ANIM_TIME = 30000;
	boolean TEXT = true;
	boolean GUI = false;

	// STATES

	public static final int NOISE = 0;
	public static final int SILENCE = 1;
	private static final int PROBABILITY_SOUNDS = 166;
	private static final boolean DEBUG_SERIAL = false;

	// int[] states = { NOISE, SILENCE };

	long nextCamAnimationEvent = 0;
	int actualSTATE;

	// UI
	ControlP5 controlsP5;

	PostFX fx;

	// CAMERAS
	// Initializing the cameraStates
	PeasyCam cam;
	CameraState camState;

	int camStatesIndex;
	JSONArray camStatesJSON, camStatesJSONImport;
	int indexJSON;

	ArrayList<CameraState> camStates = new ArrayList<CameraState>();

// ANIMATION VALUEs
	AniSequence seq;

// FONTS
	PFont f;

	Serial port; // Create object from Serial class
	float mx = 0.0f;
	boolean serialFirstContact;

	// GEOMETRY
	/// - ---------
	// noiseDetail
	// number of octaves to be used by the noise
	int octava = 2;
	// falloff factor for each octave
	float falloff = 0.0f;

	int resX, resY;

	int scl = 20;
	int w = 2500;
	int h = 1000;

	int amplitud = 100;

	int BG_CLR = 0;
	// color LINE_CLR = color(90, 255, 0);

	int LINE_CLR = color(0);

	float[][] altitud;

//actors
	ArrayList<Actor> actors = new ArrayList<Actor>();
	PGraphics off;

	float velX, velY;
	float offset = 0;
	float velocidad = 0.0f;

	String[] texts;
	GradientBackground bg;

	Texts textObject;
	SoundObj soundObject;

	float time;

	float panRot = 0;
	float tiltRot = 0;

	Actor pan, tilt;

	String[] speechFiles = { "hey.wav", "woher.wav" };
	String[] chanceFiles = { "621612__strangehorizon__sabian-20-ride-2.wav", "violin-bow-on-cymbal-a.wav" };

	// method used only for setting the size of the window
	public void settings() {
		size(WIDTH, HEIGHT, P3D);
	}

	@Override
	public void setup() {
		Ani.init(this);

		noCursor();

		// INIT TEXT

		textObject = new Texts("pearlinnoise.txt");

//		INIT VOICE
//		speech = new Speech(this, "woher.wav");
		soundObject = new SoundObj(this, speechFiles, chanceFiles);

		// INIT BG

		bg = new GradientBackground(this);

		fx = new PostFX(this);

		frameRate(25);
		// define camState
		camStatesIndex = 0;

		cam = new PeasyCam(this, 100);
		cam.setActive(false);
		cam.setWheelScale(0.2);
		camState = cam.getState();

		// visualiza los GUI y actores
		armarGUI();

		if (GEOMETRY_ANIM) {
			startAnimSeq("animSeqSteps.json");
		}

		velX = 0.1f;
		velY = 0.2f;
		resX = w / scl;
		resY = h / scl;
		println("resX = " + resX);

		println("resY = " + resY);
		altitud = new float[resX][resY];

		initActors(resY - 18);

		// List all the available serial ports, preceded by their index number:
		printArray(Serial.list());
		// Instead of 0 input the index number of the port you are using:
		// String port = findPort();
		port = new Serial(this, Serial.list()[1], 115200);
		serialFirstContact = false;
		// Whether we've heard from the
		// microcontroller;

		// FONT
		f = createFont("SourceCodePro-Regular.ttf", 114);

		// JSON CAMStates
		importCameraAnimation("camStates.json");
		// gradient

		// begin with NOISE
		setState(NOISE);

	}

	@Override
	public void draw() {

		bg.draw();

		lights();
		noiseDetail(octava, falloff);
		offset -= velocidad / 1000;
		float yoff = offset;
		for (int y = 0; y < resY; y++) {
			float xoff = 0;
			for (int x = 0; x < resX; x++) {
				// altitud[x][y] = random(-10, 10);
				altitud[x][y] = map(noise(xoff, yoff), 0.f, 1.f, -amplitud, amplitud);
				xoff += velX;
			}
			yoff += velY;
		}

		// setGradient(0, 0, width, height, c1, c2, Y_AXIS);
		stroke(LINE_CLR);
		// noFill();
		fill(200, 100);
		// fill(255, 255,255, 200);
		translate(width / 2, height / 2);
		rotateX(PI / 3);
		translate(-w / 2, -h / 2);
		for (int y = 0; y < resY - 1; y++) {
			// for everysingle row
			// beginShape(TRIANGLE_STRIP);

			beginShape(QUAD_STRIP);
			for (int x = 0; x < resX; x++) {
				vertex(x * scl, y * scl, altitud[x][y]);
				vertex(x * scl, (y + 1) * scl, altitud[x][y + 1]);
				// DISPLAY TEXT -
				float v = map(altitud[x][y], -amplitud, amplitud, 0.f, 1.f);
				if ((x % 40 == 0) && (y % 30 == 0)) {
					if (TEXT)
						text(v, x * scl, y * scl, altitud[x][y] + 10);
				}
			}
			endShape();
		}
		// only for TEXT -
		textSize(48);
		fill(255);

		switch (actualSTATE) {
		case NOISE:

			soundObject.chanceSound(PROBABILITY_SOUNDS);

			break;

		case SILENCE:

			if (soundObject.detectBeat()) {
				println("MOVE ROBOT");

				// velocidad y grados
				moveRobot(0.7f, 180);
				// poner en pausa audio - activar motor de pan&tilt

			}

			fill(255, 249, 49, 255);
			for (int y = 0; y < resY - 1; y++) {
				// for everysingle row
				// beginShape(TRIANGLE_STRIP);

				beginShape(QUAD_STRIP);
				for (int x = 0; x < resX; x++) {
					vertex(x * scl, y * scl, 0);
					vertex(x * scl, (y + 1) * scl, 0);
					// DISPLAY TEXT -

				}
				endShape();
			}

			if (!soundObject.isPlaying()) {
				// speech.parar();
				setState(NOISE);

			}

			// println(panRot);
			pan.rot = panRot;
			tilt.rot = tiltRot;

			break;

		default:
			break;
		}

		// LOOK
		fx.render().blur(2, 2.0f)
				// .chromaticAberration()
				// .vignette(1.0,0.5)
				.compose();

// Update just to 6 first actor to update height values of geometry
// the last actors are reserved for Robot Arm 
		// los primeros 6 pines reservados para las maracas en el piso
		for (int i = 0; i < 6; i++) {
			actors.get(i).update();
		}

		if (GUI) {
			showGUI();
			cursor();
		}

		//// LETTER on TOP

		hint(DISABLE_DEPTH_TEST);
		cam.beginHUD();

		if (TEXT) {
			textObject.render();
		}
		cam.endHUD();
		hint(ENABLE_DEPTH_TEST);

		if (serialFirstContact && !actors.isEmpty()) {
			// if (port.available() > 0){
			sendRotationFromActorsOverSerial();
			// }
		}

		// CAM ANIMATIONS
		if (CAM_ANIM) {
			if (millis() >= nextCamAnimationEvent) {
				// randomCameraMove();
				animateCamera();
			}
		}
	}

	public void moveRobot(float duration, int degree) {
		panRot = 0;
		tiltRot = 0;
		Ani panAni = new Ani(this, duration, .0f, "panRot", degree, Ani.LINEAR);
		// panAni.setPlayMode(Ani.YOYO);
		panAni.repeat((int) random(1, 3));
		Ani tiltAni = new Ani(this, duration, 0.f, "tiltRot", degree, Ani.EXPO_IN_OUT);
		// tiltAni.setPlayMode(Ani.YOYO);

		tiltAni.repeat((int) random(1, 3));

	}

	public void setState(int state) {
		actualSTATE = state;

		switch (actualSTATE) {
		case NOISE:
			println("state switched to NOISE");

			velocidad = 17;
			octava = 2;
			falloff = 1.06f;

			textObject.resume();
			soundObject.backgroundSound.play();

			// in 60 sec setstate SILENCE
			time = 0;
			Ani.to(this, SPEECH_TIME, "time", (int) SPEECH_TIME, Ani.LINEAR, "onEnd:setStateSilence");

			// back to init colors
			bg.resetColors();

			break;

//			SPEECH activado / los textos se van 
		case SILENCE:

			println("state switched to SILENCE");

			textObject.pause();

			soundObject.playActualSpeech();
			soundObject.backgroundSound.stop();
//			bg.complementeryColors();
//			bg.tetadricColors();
			bg.blendColor(3);

			velocidad = 0;
			falloff = 0;
			octava = 0;
			break;

		default:
			break;
		}
	}

	public void setStateSilence() {
		setState(SILENCE);
	}

	class GradientBackground {
//		 https://github.com/micycle1/PeasyGradients 
		// GR
		PeasyGradients peasyGradients;
		Gradient gradient;
		int[] resetColors = new int[4];
		int[] tetadric, complementery;
//		Ani clrAni1, clrAni2, clrAni3, clrAni4;
		Ani clrAni;
		int[] actualCrl = new int[4];
		int[] fromClr = new int[4];
		int[] toClr = new int[4];
		private float blendfactor;

		public GradientBackground(PApplet p) {

			peasyGradients = new PeasyGradients(p);

			// colorMode(HSB, 360, 100, 100);

			actualCrl[0] = color(9, 101, 133);
			actualCrl[1] = color(071, 244, 218);
			actualCrl[2] = color(11, 167, 228);
			actualCrl[3] = color(238, 85, 23);

			for (int i = 0; i < actualCrl.length; i++) {
				resetColors[i] = actualCrl[i];
				fromClr[i] = actualCrl[i];
			}
//	
			gradient = new Gradient(actualCrl[0], actualCrl[1], actualCrl[2], actualCrl[3]);
			// randomColors(4);
			gradient.primeAnimation();
			gradient.setInterpolationMode(Interpolation.SMOOTH_STEP);
			blendfactor = 0.f;

		}

		public void randomColors(int howmanyColors) {
			gradient = Gradient.randomGradient(howmanyColors);

		}

		public void resetColors() {
			blendfactor = 0;
			gradient = new Gradient(resetColors[0], resetColors[1], resetColors[2], resetColors[3]);

			// randomColors(4);
			gradient.primeAnimation();
			gradient.setInterpolationMode(Interpolation.SMOOTH_STEP);
		}

		public void complementeryColors() {
			complementery = Palette.complementary();
			gradient = new Gradient(complementery[0], complementery[1]);
			gradient.primeAnimation();
		}

		public void tetadricColors() {
			tetadric = Palette.tetradic();
			println("tetradic!");

//		
			gradient = new Gradient(tetadric[0], tetadric[1], tetadric[2], tetadric[3]);
			gradient.primeAnimation();
		}

		public void blendColor(float time) {
			blendfactor = 0;
			toClr = Palette.tetradic();

			for (int i = 0; i < fromClr.length; i++) {
				fromClr[i] = actualCrl[i];
			}

			clrAni = new Ani(this, time, 0, "blendfactor", 1.0f, Ani.EXPO_IN_OUT);

		}

		public void draw() {
			// gradient = new Gradient(clr1, clr2, clr3, clr4);

			// println(blendfactor);
			for (int i = 0; i < actualCrl.length; i++) {
				gradient.setStopColor(i, actualCrl[i]);
				actualCrl[i] = lerpColor(fromClr[i], toClr[i], blendfactor);
			}

//			intersante efecto 
			gradient = new Gradient(actualCrl[0], actualCrl[1], actualCrl[2], actualCrl[3]);
			gradient.primeAnimation();
			peasyGradients.spiralGradient(gradient, new PVector(width / 2, height / 2), (float) (frameCount * 0.02), 3);

		}
	}

	class SoundObj {

//		SoundFile file;
		SoundFile[] speechSounds;
		SoundFile backgroundSound;
		SoundFile[] chanceSound;
		int probability, chanceIndex;

		BeatDetector[] beatDetectors;
//		BeatDetector beatDetector;
		boolean active;

		// Declare a smooth factor to smooth out sudden changes in amplitude.
		// With a smooth factor of 1, only the last measured amplitude is used for the
		// visualisation, which can lead to very abrupt changes. As you decrease the
		// smooth factor towards 0, the measured amplitudes are averaged across frames,
		// leading to more pleasant gradual changes

		// Amplitude rms;
		// float smoothingFactor = 0.25f;

		// Used for storing the smoothed amplitude value
		// float sum;
		// String filestring;
		String[] speechSoundfilenames;
		String[] chanceSoundFilenames;
		int actualSound;

		SoundObj(PApplet p, String[] speechSoundFilenames, String[] chanceSoundsFilenames) {

			actualSound = 0;
			active = false;
			chanceIndex = 0;

			speechSounds = new SoundFile[speechSoundFilenames.length];
			beatDetectors = new BeatDetector[speechSoundFilenames.length];
			chanceSound = new SoundFile[chanceSoundsFilenames.length];

			speechSoundfilenames = speechSoundFilenames;
			chanceSoundFilenames = chanceSoundsFilenames;

			for (int i = 0; i < speechSoundFilenames.length; i++) {
				SoundFile thisSound = new SoundFile(p, speechSoundfilenames[i]);
				thisSound.stop();
				speechSounds[i] = thisSound;
				beatDetectors[i] = new BeatDetector(p);
				beatDetectors[i].input(speechSounds[i]);
				beatDetectors[i].sensitivity(BEAT_SENTIVITY);

				// PeakAmpltude
				// Create and patch the rms tracker
//				rms = new Amplitude(p);
//				rms.input(thisSound);
			}

			for (int i = 0; i < chanceSoundsFilenames.length; i++) {
				chanceSound[i] = new SoundFile(p, chanceSoundsFilenames[i]);
				chanceSound[i].stop();

			}

			backgroundSound = new SoundFile(p, "agua.wav");
			// backgroundSound.play();
			backgroundSound.loop();
			backgroundSound.pause();

		}

		public boolean detectBeat() {
//			boolean beat = beatDetector.isBeat();
//			println(actualSound);
			boolean beat = beatDetectors[actualSound].isBeat();
			return beat;
		}

		public void chanceSound(int prob) {
			probability = (int) random(prob);
			// println(probability);

			if (random(probability) == 0) {
				// println(chanceIndex);
				SoundFile thisSound = chanceSound[chanceIndex];
				if (!thisSound.isPlaying()) {
					thisSound.play();
					println("chance sound! " + thisSound.toString());

				}
				chanceIndex = (chanceIndex + 1) % chanceSound.length;
			}
		}

//		public float peakAmplitude() {
//			// smooth the rms data by smoothing factor
//			sum += (rms.analyze() - sum) * smoothingFactor;
//			return sum;
//		}

		public void playActualSpeech() {

			if (!speechSounds[actualSound].isPlaying())

				println("play " + speechSoundfilenames[actualSound]);
			speechSounds[actualSound].play();
			speechSounds[actualSound].add(.3f);
			active = true;
		}

		public void comienzo() {
			speechSounds[actualSound].jump(0);
		}

		public void pausa() {
			speechSounds[actualSound].pause();
		}

		public void parar() {
			speechSounds[actualSound].stop();
			active = false;
		}

		public boolean isPlaying() {
			boolean p = speechSounds[actualSound].isPlaying();

			if (!p) {
				active = false;
				actualSound = (actualSound + 1) % speechSounds.length;
//				beatDetectors[actualSound].input(speechSounds[actualSound]);
//				beatDetectors[actualSound].sensitivity(BEAT_SENTIVITY);
			}
			return p;
		}

	}

	class Texts {
		String[] lines;
		float x, y;
		PFont f;
		int actualIndex;
		float fadeIn, fadeOut, firstDelay;
		float sustain;
		float alpha;
		Ani fadeInAni, fadeOutAni;
		int fontsize;
		private boolean active;

		Texts(String file) {
			x = width * 0.5f;
			y = height * 0.5f;
			f = createFont("SourceCodePro-Regular.ttf", 114);
			active = true;
			lines = loadFile(file);
			actualIndex = 0;
			fadeOut = 2.0f;
			fadeIn = 3.f;
			alpha = 0.f;
			// sustain = 7.f;
			// sustain depends on leght onf line
			sustain = lines[actualIndex].length() / 2;
			firstDelay = 0.f;
			// Ani.to(this, fadeIn, "alpha", 255.f, Ani.EXPO_IN_OUT, "onEnd:fadeOutAfter");
			fadeInAni = new Ani(this, fadeIn, firstDelay, "alpha", 255.f, Ani.EXPO_IN_OUT, "onEnd:fadeOutAfter");
			fontsize = (int) (height * 0.07f);

		}

		void fadeOutAfter() {
			// println("fadeOut");
			// exec ("say","-v", "Paulina",lines[actualIndex]);
			if (TEXT2SPEECH && active)
				exec("say", "-v", "Kate", lines[actualIndex]);
			Ani.to(this, fadeOut, sustain, "alpha", 0, Ani.EXPO_IN_OUT, "onEnd:nextLine");
		}

		public void pause() {
//			fadeOutAni = new Ani(this, fadeOut, 0, "alpha", 0, Ani.LINEAR);
			active = false;
		}

		public void resume() {
			active = true;
//			Ani.to(this, fadeIn,0, "alpha", 255.f, Ani.EXPO_IN, "onEnd:fadeOutAfter");
			// fadeInAni.repeat();
		}

		public void nextLine() {
			// println("nextLine");
			actualIndex++;
			if (actualIndex >= lines.length) {
				actualIndex = 0;
			}
			sustain = lines[actualIndex].length() / 10.f;

			// println("sustain: " + sustain);
			Ani.to(this, fadeIn, 2.f, "alpha", 255.f, Ani.EXPO_IN_OUT, "onEnd:fadeOutAfter");

		}

		public void render() {
			if (active) {

				pushMatrix();
				translate(x, y);
				textFont(f, fontsize);
				fill(255, alpha);
				textAlign(CENTER);
				text(lines[actualIndex], 0, 0);
				popMatrix();
			}

			// println(alpha);
		}

		public String[] loadFile(String txt) {
			String[] lines = loadStrings(txt);
			println("text loaded with" + lines.length + " lines");
			return lines;
		}

	}

	public void initActors(int total) {
		// Setup Actors
		int index = 0;
		for (int y = 0; y < total; y++) {
			// for every single row
			for (int x = 0; x < resX; x++) {
				if ((x % 40 == 0) && (y % 30 == 0)) {
					float offY = 200;
					float offX = width * 0.3f;
					actors.add(new Actor(x, y, offX, offY, index));
					index++;
				}
			}
		}
		println("size of actors Array: " + actors.size());

		// TO move the robot
//		PIN 8 in ARDUINO 
		pan = actors.get(6);
//		PIN 9 in ARDUINO 
		tilt = actors.get(7);
	}
//// SETUP ------------------------------
/// - - - - - - - - - - - - - - - - - - -

	public void serialEvent(Serial port) {

		// if this is the first byte received, and it's an A,
		// clear the serial buffer and note that you've
		// had first contact from the microcontroller.
		// Otherwise, read the incoming String to inBuffer:
		//

		// port.bufferUntil(10);
		if (!serialFirstContact) {
			// read a byte from the serial port:
			int inByte = port.read();
			if (inByte == 'X') {
				// port.clear(); // clear the serial port buffer
				serialFirstContact = true; // you've had first contact from the
				// microcontroller
				println("connected to : " + port);
			}
		} else {
			String inBuffer = port.readString();
			if (inBuffer != null) {
				if (DEBUG_SERIAL)
					print(inBuffer);

			}
		}
	}

	public void sendRotationFromActorsOverSerial() {
		String str = "s ";

		for (int i = 0; i < actors.size(); i++) {
			if (i > 0) {
				str += " ";// We add a comma before each value, except the first value
			}
			Actor thisActor = actors.get(i);

			int RotationToSerial = (int) map(thisActor.rot, 0, 180, 0, 180);
			// println(thisActor.rot);
			str += str(RotationToSerial);// We concatenate each number in the string.
			// println(str);
		}

		// while (port.available() > 0) {
		port.write(str);
		port.write(13);

//		 println(str);

//		}
	}

	private void importCameraAnimation(String json) {
		camStatesJSON = new JSONArray();
		indexJSON = 0;
		camStatesJSONImport = loadJSONArray(json);

		// println(camStatesJSONImport);
		println(json + " is loaded!. ");

		for (int i = 0; i < camStatesJSONImport.size(); i++) {
			JSONObject thisCamState = camStatesJSONImport.getJSONObject(i);
			int id = thisCamState.getInt("id");
			JSONArray posJSON = thisCamState.getJSONArray("position");
			float[] pos = { posJSON.getFloat(0), posJSON.getFloat(1), posJSON.getFloat(2) };
			JSONArray rotJSON = thisCamState.getJSONArray("rotation");
			float[] rot = { rotJSON.getFloat(0), rotJSON.getFloat(1), rotJSON.getFloat(2) };
			float dist = thisCamState.getFloat("distance");
			Rotation Rota = new Rotation(RotationOrder.XYZ, rot[0], rot[1], rot[2]);
			Vector3D center = new Vector3D(pos[0], pos[1], pos[2]);
			camStates.add(new CameraState(Rota, center, dist));
		}
	}

	private void startAnimSeq(String jsonfile) {
		// TODO Auto-generated method stub

		JSONArray jsonArray = loadJSONArray(jsonfile);
		// println(jsonArray);
		println(jsonfile + " is loaded!. ");
		seq = new AniSequence(this);
		seq.beginSequence();

		for (int i = 0; i < jsonArray.size(); i++) {
			JSONObject thisStep = jsonArray.getJSONObject(i);
			int dur = thisStep.getInt("duration");
			JSONArray animationVariables = thisStep.getJSONArray("animate");

			String string = null;
			for (int j = 0; j < animationVariables.size(); j++) {
				JSONObject js = animationVariables.getJSONObject(j);
				if (j == 0) {
					string = js.getString("variable");
					string += ":";
					float value = js.getFloat("to");
					string += str(value);

				} else {
					string += ",";
					string += js.getString("variable");
					string += ":";
					float value = js.getFloat("to");
					string += str(value);
				}

			}
			// println(string);

			seq.add(Ani.to(this, dur, string));

		}

		// step 1
		// seq.add(Ani.to(this, 10, "falloff", 2));

		seq.endSequence();

		seq.start();

	}

	public void setAnimSeq() {

//		 create a sequence
//		 dont forget to call beginSequence() and endSequence()
		seq = new AniSequence(this);
		seq.beginSequence();

		// step 0
		seq.add(Ani.to(this, 10, "octava:2,velocidad:30.0"));

		// step 1
		seq.add(Ani.to(this, 10, "falloff", 2));

		seq.endSequence();

		// start the whole sequence
		seq.start();
	}

	public void animateCamera() {

		if (camStatesIndex >= camStates.size()) {
			camStatesIndex = 0;
		}

		cam.setState(camStates.get(camStatesIndex), ANIM_TIME);
		nextCamAnimationEvent = millis() + ANIM_TIME;
		camStatesIndex++;
	}

	public void randomCameraMove() {

		// avanza 1 o 2 cámaras
		camStatesIndex += (int) random(1, 3);
		// Si nos pasamos del máx, retrocede una "página"
		if (camStatesIndex > 2) {
			camStatesIndex -= 3;
		}
		if (camStatesIndex == 0) {
			cam.setState(camStates.get(0), 2000);
		} else if (camStatesIndex == 1) {
			cam.setState(camStates.get(1), 2000);
		} else if (camStatesIndex == 2) {
			cam.setState(camStates.get(2), 2000);
		}
		nextCamAnimationEvent = millis() + 2000;
		// println(r, cam.getState());
	}

	public void showGUI() {

		hint(DISABLE_DEPTH_TEST);
		cam.beginHUD();
		controlsP5.draw();

		textSize(48);
		fill(255);

		for (Actor a : actors) {
			// a.update();
			a.render();
		}

		// Monitor Variables
		textAlign(LEFT);
		text("falloff: " + falloff, 50, height * 0.8f);
		text("octava: " + octava, 50, height * 0.85f);
		text("vel: " + velocidad, 50, height * 0.9f);
		text("cameraAnimation Step: " + camStatesIndex, 50, height * 0.95f);

		// render Actors
		// text(pos[0], 10, 10);
		cam.endHUD();
		hint(ENABLE_DEPTH_TEST);
	}

	@Override
	public void keyReleased() {
		if (key == '1')
			cam.setActive(true);
		if (key == '2')
			cam.setActive(false);
		if (key == 's') {
			camState = cam.getState();
			float[] pos = cam.getPosition();
			// println("position : " + pos[0] + "," + pos[1] + "," + pos[2]);

			float[] look = cam.getLookAt();
			// println("lookAt : " + look[0] + "," + look[1] + "," + look[2]);
			double dist = cam.getDistance();
			// println("distance : " + dist);
			float[] rotations = cam.getRotations();
			// println(rotations[0] + " , " + rotations[1] + " , " + rotations[2]);

			JSONObject json = new JSONObject();

			json.setInt("id", indexJSON);
			// JSONArray jsonArray = new JSONArray();
			JSONArray rot = new JSONArray();
			rot.append(rotations[0]);
			rot.append(rotations[1]);
			rot.append(rotations[2]);
			json.setJSONArray("rotation", rot);
			JSONArray centerjson = new JSONArray();
			centerjson.append(pos[0]);
			centerjson.append(pos[1]);
			centerjson.append(pos[2]);
			json.setJSONArray("position", centerjson);
			json.setFloat("distance", (float) dist);

			// println(json);

			camStatesJSON.setJSONObject(indexJSON, json);
			indexJSON++;
			println(camStatesJSON);

			saveJSONArray(camStatesJSON, "data / camStates.json");
		}
		if (key == 'r')
			cam.setState(camState, 1000);
		if (key == 't')
			TEXT = !TEXT;
		if (key == 'a')
			GUI = !GUI;

		if (key == 'g')
			// bg.randomColors(36);
			bg.tetadricColors();
		
		if ( key == 'q') {
			println("ESC");
			port.clear();
			port.stop();
			exit(); 
			
		}
	}

	class Actor {
		float posX, posY;
		int x, y;
		boolean active;
		float rot, rotVel, rotAim;
		float rotRadians;
		int index;
		float slide;
		// float value;
		// float offX,offY;

		Actor(int _x, int _y, float offX, float offY, int i) {
			active = false;

			x = _x;
			y = _y;
			println("actor created at : " + x + " , " + y);
			posX = x * 5 + offX;
			posY = y * 5 + offY;
			// posY = y;
			index = i;
			rot = 0;
			// rotAim = rot;
			// rotVel = random(0.01f, 0.05f);
			// slide = .03f;
		}

		public void update() {

			// rotAim =
			// float diff = abs(rot - rotAim);

			// rot += diff * slide;
			// rot += rotVel;
			// value = map(altitud[x][y], -100, 100, 0, 1.0);
			float value = map(altitud[x][y] * 8, -amplitud, amplitud, 0, 180);
			if (value > 180) {
				value = 180;
			} else if (value < 0) {
				value = 0;
			}
			rot = value;

		}

		void setActive(boolean b) {
			active = b;
		}

		void rotateTo(float r) {
			rotAim = r;
		}

		void render() {
//			noFill();
			fill(30, 255, 235);
			pushMatrix();
			translate(posX, posY);

			// Monitor the rotation VALUE
			text(nf(rot, 0, 1), 0, 50);
			// ellipseMode(CENTER);
			// ellipse(0, 0, 15, 15);
			rectMode(CENTER);
			stroke(255);
			rotateZ(radians(rot));
			// rotateZ(rotRadians);

			rect(0, 0, 100, 5);
			popMatrix();
			textSize(38);
			textAlign(CENTER, BOTTOM);
			text(index, posX, posY);
		}
	}

	class Timer {
		float now, time;
		boolean STOP;

		Timer(float _time) {
			now = _time;
			time = _time;
			STOP = true;
		}

		public float getNow() {
			return now;

		}

		public void set(float time) {
			now = time;
			STOP = false;
			println("set timer for " + now);

		}

		public void up() {

			now += 1 / frameRate;
		}

		public void down() {
			if (now >= 0) {

				now -= 1 / frameRate;

			} else {
				STOP = true;
			}
		}

		public void reset() {
			STOP = false;
			now = time;
			println("reset timer for " + now);

		}

		public boolean isOver() {
			return STOP;
		}

	}

	public void armarGUI() {
		// CTRL
		controlsP5 = new ControlP5(this);
		controlsP5.addSlider("octava").setPosition(50, 50).setRange(0, 8).setId(1);
		controlsP5.addSlider("falloff").setPosition(50, 70).setRange(0, 1.5f).setId(2);
		controlsP5.addSlider("velocidad").setPosition(50, 90).setRange(0, 20).setId(3);
		controlsP5.addSlider("scl").setPosition(50, 110).setRange(10, 1000).setId(4);
		controlsP5.setAutoDraw(false);

		controlsP5.getController("octava").getCaptionLabel().setColor(color(255, 0, 0));
		controlsP5.getController("falloff").getCaptionLabel().setColor(color(255, 0, 0));
		controlsP5.getController("velocidad").getCaptionLabel().setColor(color(255, 0, 0));
		controlsP5.getController("scl").getCaptionLabel().setColor(color(255, 0, 0));

	}

}