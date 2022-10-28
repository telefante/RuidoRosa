
//import java.io.Serial;
import java.util.ArrayList;

import ch.bildspur.postfx.builder.PostFX;
import controlP5.ControlP5;
import de.looksgood.ani.Ani;
import de.looksgood.ani.AniSequence;
import micycle.peasygradients.PeasyGradients;
import micycle.peasygradients.gradient.Gradient;
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
import processing.opengl.*;

//ARDUINO SEriaL
import processing.serial.*;

public class Ruidoperla extends PApplet {

	public static void main(String[] args) {
		PApplet.main("Ruidoperla");
	}

// GR
	PeasyGradients peasyGradients;
	Gradient grad;

// UI
	ControlP5 cp5;

	PostFX fx;

// CAMERAS
//Initializing the cameraStates
	PeasyCam cam;
	CameraState state;

	boolean CAM_ANIM = true;
	int ANIM_TIME = 10000;
	long nextEvent = 0;
	int r;
	JSONArray camStatesJSON, camStatesJSONImport;
	int indexJSON;

	ArrayList<CameraState> camStates = new ArrayList<CameraState>();

// ANIMATION VALUEs
	AniSequence seq;

// FONTS
	PFont f;

	Serial port; // Create object from Serial class
	float mx = 0.0f;
	boolean firstContact;

// GEOMETRY
///- ---------
//noiseDetail
//number of octaves to be used by the noise
	int octava = 1;
//falloff factor for each octave
	float falloff = 0.9f;

	int resX, resY;

	int scl = 20;
	int w = 2500;
	int h = 1000;

	int amplitud = 100;

	int BG_CLR = 0;
// color LINE_CLR = color(90, 255, 0);
	int LINE_CLR = color(0);

// VIsualize
	boolean TEXT = true;
	boolean GUI = true;

	float[][] altitud;

//actors
	ArrayList<Actor> actors = new ArrayList<Actor>();
	PGraphics off;

	float velX, velY;
	float offset = 0;
	float velocidad = 0.02f;

	// method used only for setting the size of the window
	public void settings() {
		size(1280, 720, OPENGL);
	}
//// SETUP ------------------------------
/// - - - - - - - - - - - - - - - - - - -

	@Override
	public void setup() {

		fx = new PostFX(this);

		frameRate(25);
		// define camState
		// r = int(random(0, 3));
		r = 0;
		// off = createGraphics(600,600);
		size(1280, 720, OPENGL);

		cam = new PeasyCam(this, 100);
		cam.setActive(false);
		cam.setWheelScale(0.2);
		state = cam.getState();

		// visualiza los GUI y actores
		armarGUI();

		// Set ANim Seq
		Ani.init(this);
		setAnimSeq();

		velX = 0.1f;
		velY = 0.2f;
		resX = w / scl;
		resY = h / scl;
		altitud = new float[resX][resY];

		// Setup Actors
		int index = 0;
		for (int y = 0; y < resY - 1; y++) {
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

		// Open the port that thrÏe board is connected to and use the same speed (9600
		// bps)
		// port = new Serial(this, 9600); // Comment this line if it's not the correct
		// port
		// Ifthe above does not work uncomment the lines below to choose the correct
		// port
		// List all the available serial ports, preceded by their index number:
		printArray(Serial.list());
		// Instead of 0 input the index number of the port you are using:

		// String port = findPort();
		port = new Serial(this, Serial.list()[1], 115200);
		firstContact = false;
		// Whether we've heard from the
		// microcontroller;

		// FONT
		f = createFont("SourceCodePro-Regular.ttf", 72);

		// JSON CAMStates
		camStatesJSON = new JSONArray();
		indexJSON = 0;
		camStatesJSONImport = loadJSONArray("camStates.json");

		println(camStatesJSONImport);
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
		// gradient

		peasyGradients = new PeasyGradients(this);
		// grad = new Gradient(color(255,120,170), color(252,245,140));
		// grad = new Gradient(color(255,100,100), color(252,245,140));
		grad = new Gradient(color(9, 101, 133), color(071, 244, 218), color(11, 167, 228), color(238, 85, 23),
				color(112, 12, 25));
		grad.primeAnimation();
		grad.setInterpolationMode(Interpolation.SMOOTH_STEP);
	}

	@Override
	public void draw() {
		// background(BG_CLR);
		// peasyGradients.linearGradient(grad, 45); // angle = 0 (horizontal)
		peasyGradients.spiralGradient(grad, new PVector(width / 2, height / 2), (float) (frameCount * 0.02), 3);

		// peasyGradients.noiseGradient(grad, new PVector(width*0.5,height*0.5),45,1.0);
		// // angle = 0 (horizontal)

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

		fx.render().blur(2, 2.0f)
				// .chromaticAberration()
				// .vignette(1.0,0.5)
				.compose();

		if (GUI) {
			gui();
		}

		//// LETTER on TOP

		hint(DISABLE_DEPTH_TEST);
		cam.beginHUD();
		fill(255, 255, 0);
		textFont(f);
		if (TEXT) {
			text("contemplate", width / 2, height / 2);
		}
		cam.endHUD();
		hint(ENABLE_DEPTH_TEST);

		if (firstContact) {
			// if (port.available() > 0){
			sendSerial();
			// }
		}

		// CAM ANIMATIONS
		if (CAM_ANIM) {
			if (millis() >= nextEvent) {
				// randomCameraMove();
				animateCamera();
			}
		}
	}

	public void setAnimSeq() {

		// create a sequence
		// dont forget to call beginSequence() and endSequence()
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

		if (r >= camStates.size()) {
			r = 0;
		}

		cam.setState(camStates.get(r), ANIM_TIME);
		nextEvent = millis() + ANIM_TIME;
		r++;
	}

	public void randomCameraMove() {

		// avanza 1 o 2 cámaras
		r += (int) random(1, 3);
		// Si nos pasamos del máx, retrocede una "página"
		if (r > 2) {
			r -= 3;
		}
		if (r == 0) {
			cam.setState(camStates.get(0), 2000);
		} else if (r == 1) {
			cam.setState(camStates.get(1), 2000);
		} else if (r == 2) {
			cam.setState(camStates.get(2), 2000);
		}
		nextEvent = millis() + 2000;
		// println(r, cam.getState());
	}

	public void sendSerial() {
		String str = "s ";

		for (int i = 0; i < actors.size(); i++) {
			if (i > 0) {
				str += " ";// We add a comma before each value, except the first value
			}
			Actor thisActor = actors.get(i);
			// float grad = int(degrees(thisActor.rot));
			// float serRot = map(grad, 0,360, 0, 180);
			float serRot = map(thisActor.rot, 0, 360, 0, 360);
			// println(thisActor.rot);
			str += (int) thisActor.rot;// We concatenate each number in the string.
			// println(str);
		}

		while (port.available() > 0) {
			port.write(str);
			port.write(13);

			// println(str);

		}
	}

	public void serialEvent(Serial port) {

		// if this is the first byte received, and it's an A,
		// clear the serial buffer and note that you've
		// had first contact from the microcontroller.
		// Otherwise, read the incoming String to inBuffer:
//
		if (!firstContact) {
			// read a byte from the serial port:
			int inByte = port.read();
			if (inByte == 'X') {
				// port.clear(); // clear the serial port buffer
				firstContact = true; // you've had first contact from the
				// microcontroller
				println("connected to : " + port);
			}
		} else {
			String inBuffer = port.readString();
			if (inBuffer != null) {
				print(inBuffer);
			}
		}
	}

	public void gui() {

		hint(DISABLE_DEPTH_TEST);
		cam.beginHUD();
		cp5.draw();

		textSize(48);
		fill(255);

		for (Actor a : actors) {
			a.update();
			a.render();
		}

		// Monitor Variables
		textAlign(LEFT);
		text("falloff: " + falloff, 50, height * 0.8f);
		text("octava: " + octava, 50, height * 0.85f);
		text("vel: " + velocidad, 50, height * 0.9f);
		text("cameraAnimation Step: " + r, 50, height * 0.95f);

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
			state = cam.getState();
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
			cam.setState(state, 1000);
		if (key == 'g')
			println(cam.getState());
		if (key == 't')
			TEXT = !TEXT;
		if (key == 'a')
			GUI = !GUI;
	}

	class Actor {
		float posX, posY;
		int x, y;
		boolean active;
		float rot, rotVel, rotAim;
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
			rotAim = rot;
			rotVel = random(0.01f, 0.05f);
			slide = .03f;
		}

		public void update() {

			// rotAim =
			float diff = abs(rotAim - rot);

			rot += diff * slide;
			// rot += rotVel;
			// value = map(altitud[x][y], -100, 100, 0, 1.0);
			rot = map(altitud[x][y], -100, 100, 0, 360);
			// port.write(int(rot)); // Write the angle to the serial port
			// println(rot);
		}

		void setActive(boolean b) {
			active = b;
		}

		void rotateTo(float r) {
			rotAim = r;
		}

		void render() {
			noFill();
			pushMatrix();
			translate(posX, posY);

			// translate(0,-50);
			// float v = map(altitud[x][y], -100, 100, 0, 360);
			// text(v, 0,50);
			text(rot, 0, 50);
			ellipseMode(CENTER);
			ellipse(0, 0, 15, 15);
			rectMode(CENTER);
			stroke(255);
			rotateZ(radians(rot));
			rect(0, 0, 100, 5);
			popMatrix();
			textSize(38);
			textAlign(CENTER, BOTTOM);
			text(index, posX, posY);
		}
	}

	public void armarGUI() {
		// CTRL
		cp5 = new ControlP5(this);
		cp5.addSlider("octava").setPosition(50, 50).setRange(0, 18).setId(1);
		cp5.addSlider("falloff").setPosition(50, 70).setRange(0, 1.1f).setId(2);
		cp5.addSlider("velocidad").setPosition(50, 90).setRange(0, 10).setId(3);
		cp5.addSlider("scl").setPosition(50, 110).setRange(10, 1000).setId(4);
		cp5.setAutoDraw(false);

		cp5.getController("octava").getCaptionLabel().setColor(color(255, 0, 0));
		cp5.getController("falloff").getCaptionLabel().setColor(color(255, 0, 0));
		cp5.getController("velocidad").getCaptionLabel().setColor(color(255, 0, 0));
		cp5.getController("scl").getCaptionLabel().setColor(color(255, 0, 0));
		// reposition the Label for controller 'slider'
		// cp5.getController("slider").getValueLabel().align(ControlP5.LEFT,
		// ControlP5.BOTTOM_OUTSIDE).setPaddingX(0);
		// cp5.getController("slider").getCaptionLabel().align(ControlP5.RIGHT,
		// ControlP5.BOTTOM_OUTSIDE).setPaddingX(0);
	}

}