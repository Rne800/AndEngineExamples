package org.anddev.andengine.examples.benchmark;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.handler.timer.ITimerCallback;
import org.anddev.andengine.engine.handler.timer.TimerHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;

import android.hardware.SensorManager;
import android.view.MotionEvent;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

/**
 * @author Nicolas Gramlich
 * @since 18:47:08 - 19.03.2010
 */
public class PhysicsBenchmark extends BaseBenchmark implements IOnSceneTouchListener {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CAMERA_WIDTH = 720;
	private static final int CAMERA_HEIGHT = 480;
	
	private static final int COUNT_HORIZONTAL = 17;
	private static final int COUNT_VERTICAL = 15;

	// ===========================================================
	// Fields
	// ===========================================================

	private Texture mTexture;

	private TiledTextureRegion mBoxFaceTextureRegion;
	private TiledTextureRegion mCircleFaceTextureRegion;

	private PhysicsWorld mPhysicsWorld;
	private int mFaceCount = 0;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected int getBenchmarkID() {
		return PHYSICSBENCHMARK_ID;
	}

	@Override
	protected float getBenchmarkStartOffset() {
		return 2;
	}

	@Override
	protected float getBenchmarkDuration() {
		return 20;
	}

	@Override
	public Engine onLoadEngine() {
		final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new Engine(new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera));
	}

	@Override
	public void onLoadResources() {
		this.mTexture = new Texture(64, 64, TextureOptions.BILINEAR);
		TextureRegionFactory.setAssetBasePath("gfx/");
		this.mBoxFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "boxface_tiled.png", 0, 0, 2, 1); // 64x32
		this.mCircleFaceTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "circleface_tiled.png", 0, 32, 2, 1); // 64x32
		this.mEngine.getTextureManager().loadTexture(this.mTexture);
	}

	@Override
	public Scene onLoadScene() {
		final Scene scene = new Scene(2, true, 4, (COUNT_VERTICAL - 1) * (COUNT_HORIZONTAL - 1));
		scene.setBackgroundColor(0, 0, 0);
		scene.setOnSceneTouchListener(this);

		this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, 2 * SensorManager.GRAVITY_EARTH), false);

		final Shape ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
		final Shape roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
		final Shape left = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
		final Shape right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);

		PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody);
		PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody);

		scene.getBottomLayer().addEntity(ground);
		scene.getBottomLayer().addEntity(roof);
		scene.getBottomLayer().addEntity(left);
		scene.getBottomLayer().addEntity(right);
		
		for(int x = 1; x < COUNT_HORIZONTAL; x++) {
			for(int y = 1; y < COUNT_VERTICAL; y++) {
				final float pX = (((float)CAMERA_WIDTH) / COUNT_HORIZONTAL) * x + y;
				final float pY = (((float)CAMERA_HEIGHT) / COUNT_VERTICAL) * y;
				this.addFace(scene, pX - 16, pY - 16);
			}
		}

		scene.registerPreFrameHandler(new TimerHandler(2, new ITimerCallback() {
			@Override
			public void onTimePassed(final TimerHandler pTimerHandler) {
				scene.unregisterPreFrameHandler(pTimerHandler);
				scene.registerPreFrameHandler(PhysicsBenchmark.this.mPhysicsWorld);
				scene.registerPreFrameHandler(new TimerHandler(10, new ITimerCallback() {
					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						PhysicsBenchmark.this.mPhysicsWorld.setGravity(new Vector2(0, -SensorManager.GRAVITY_EARTH));
					}
				}));
			}
		}));

		return scene;
	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
		if(this.mPhysicsWorld != null) {
			if(pSceneTouchEvent.getAction() == MotionEvent.ACTION_DOWN) {
				this.addFace(pScene, pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
				return true;
			}
		}
		return false;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private void addFace(final Scene pScene, final float pX, final float pY) {
		this.mFaceCount++;

		final AnimatedSprite face;
		final Body body;
		
		if(this.mFaceCount % 2 == 0) {
			face = new AnimatedSprite(pX, pY, this.mBoxFaceTextureRegion);
			body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, face, BodyType.DynamicBody);
		} else {
			face = new AnimatedSprite(pX, pY, this.mCircleFaceTextureRegion);
			body = PhysicsFactory.createCircleBody(this.mPhysicsWorld, face, BodyType.DynamicBody);
		}
		
		face.setUpdatePhysics(false);
		
		pScene.getTopLayer().addEntity(face);
		this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body, true, true, false, false));
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}