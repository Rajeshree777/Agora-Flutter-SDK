var isTouched = false;

var scale = 0.08; // posX, posY, scaleX, scaleY
var speedRatio = 5.;

var delay = 4000; // ms

var cloverCollisionSize = scale;

var playFieldCenter = {
	x: 0.0,
    y: 0.0
};

var playFieldOffset = {
	x: 0.8,
	y: 0.3
};

var settings = {
	effectName: "HeartsLut"
};

var spendTime = 0;

var analytic = {
	spendTimeSec: 0,
	swipes: 0,
	soundButtonTouches: 0,
   	videoCountOnMasks: {
        subEffect1: {soundOnCount: 0, soundOffCount: 0},
    },
};

function sendAnalyticsData() {
	var _analytic;
	analytic.spendTimeSec = Math.round(spendTime / 1000);
	_analytic = {
		'Event Name': 'Effects Stats',
		'Effect Name': settings.effectName,
		'Effect Action': 'Swipe',
		'Action Count': String(analytic.swipes),
		'Spend Time': String(analytic.spendTimeSec),
		'Sound button touches': String(analytic.soundButtonTouches),
	   	'Video Analytics SoundOn': String(analytic.videoCountOnMasks.subEffect1.soundOnCount),
	   	'Video Analytics SoundOff': String(analytic.videoCountOnMasks.subEffect1.soundOffCount),
	};
	Api.print('sended analytic: ' + JSON.stringify(_analytic));
	Api.effectEvent('analytic', _analytic);
}

function onStop() {
	try {
		sendAnalyticsData();
	} catch (err) {
		Api.print(err);
	}
}

function onFinish() {
	try {
		sendAnalyticsData();
	} catch (err) {
		Api.print(err);
	}
}

var effectData = {
	final_luts: [
		"cream.png",
		"FilmPrint.png",
		"LC1.png",
		"LC2.png",
		"LC6.png",
		"LC10.png",
		"Portrait1.png",
		"Portrait3.png"
	]
};

var spendTime = 0;
var deltaTime = 0;
var current_lut = 0;

Math.clamp = function (value, min, max) {
	return Math.min(Math.max(value, min), max);
};

function smoothMove(x, target) {
	range = target - x;
	return x + range * 0.3;
}

function Effect() {
	var self = this;

	this.init = function() {
		Api.meshfxMsg("spawn", 1, 0, "!glfx_FACE");
		Api.meshfxMsg("spawn", 0, 0, "planes.bsm2");

		Api.meshfxMsg("spawn", 2, 0, "plane2.bsm2");

		Api.meshfxMsg("spawn", 5, 0, "quad_R3.bsm2");
        Api.meshfxMsg("tex", 5, 0, "sound_on.png");

        Api.meshfxMsg("shaderVec4", 0, 0, playFieldOffset.x + " " + playFieldOffset.y + " " + scale + " 0");

		timeOut(3000, function(){ deleteHint();});

		Api.playVideo("frx", true, 1.0);

		Api.playSound("music1.ogg", true, 1);

		saveVideoAnalytics();

		Api.showRecordButton();
	};

	this.delButton = function(){
        Api.meshfxMsg("del", 5);
    }

	this.restart = function() {
		Api.meshfxReset();
		Api.stopVideo("frx");
		Api.stopSound("music1.ogg");
		self.init();
	};

	this.timeUpdate = function () { 
		if (self.lastTime === undefined) self.lastTime = (new Date()).getTime();
	
		var now = (new Date()).getTime();
		self.delta = now - self.lastTime;
		if (self.delta < 3000) { // dont count spend time if application is minimized
			spendTime += self.delta;
		}
		self.lastTime = now;
	};

	this.faceActions = [this.timeUpdate,scalePlus];
	this.noFaceActions = [this.timeUpdate];

	this.videoRecordStartActions = [deleteHint,saveVideoAnalytics,this.delButton];
	this.videoRecordFinishActions = [];
	this.videoRecordDiscardActions = [this.restart];
}

configure(new Effect());

function onTakePhotoStart(){
	deleteHint();
	Api.meshfxMsg("del", 5);
};

var lastFingerPosX;
var fingerVelocity = 0;

var onTouchesBeganActions = [];
var onTouchesMovedActions = [];
var onTouchesEndedActions = [];

var counter = 0;

function deleteHint(){
	Api.meshfxMsg("del", 2);
}

function saveVideoAnalytics() {
	isTouched ? analytic.videoCountOnMasks.subEffect1.soundOffCount++ : analytic.videoCountOnMasks.subEffect1.soundOnCount++;
}

function onTouchesBegan(touches) {
	deleteHint();
	lastFingerPosX = touches[0].x;

	var pos = getCloverPos(0, 0);
    if (collisionCheck(touches[0].x, touches[0].y * 1.5, pos.x, pos.y, cloverCollisionSize)) {
        Api.print('You touched:' + pos.x + ', ' + pos.y);
        ++analytic.soundOnOffs;
        if(!isTouched){
            Api.setSoundVolume("01_music.ogg", 0);
            Api.meshfxMsg("tex", 5, 0, "sound_off.png");
            isTouched = true;
        } else {
            Api.setSoundVolume("01_music.ogg", 1);
            Api.meshfxMsg("tex", 5, 0, "sound_on.png");
            isTouched = false;
        }
        return;
    }

	for (var i = 0; i < onTouchesBeganActions.length; i++) {
		onTouchesBeganActions[i](touches);
	}
}

function onTouchesMoved(touches) {
	fingerVelocity = (touches[0].x - lastFingerPosX);

	for (var i = 0; i < onTouchesMovedActions.length; i++) {
		onTouchesMovedActions[i](touches);
	}

	lastFingerPosX = touches[0].x;
}

function onTouchesEnded(touches) {
	for (var i = 0; i < onTouchesEndedActions.length; i++) {
		onTouchesEndedActions[i](touches);
	}
}

function scalePlus(){
    scale += 0.001 * speedRatio;
    if (scale >= 0.17) {
        var idx = effect.faceActions.indexOf(scalePlus);
        effect.faceActions.splice(idx, 1);

        effect.faceActions.push(scaleMinus);
    }
    Api.meshfxMsg("shaderVec4", 0, 0, playFieldOffset.x + " " + playFieldOffset.y + " " + scale + " 0");
};

function scaleMinus(){
    scale -= 0.001 * speedRatio;;
    if (scale <= 0.15) {
        var idx = effect.faceActions.indexOf(scaleMinus);
        effect.faceActions.splice(idx, 1);
        if(counter < 1) {
            effect.faceActions.push(scalePlus);
        } else {
            timeOut(delay, function(){
                effect.faceActions.push(scalePlus);
                counter = 0;
            });
        }
        counter++;
    }
    Api.meshfxMsg("shaderVec4", 0, 0, playFieldOffset.x + " " + playFieldOffset.y + " " + scale + " 0");
};

function getCloverPos(x, y) {
	var pos = {};
	switch (x) {
		case 0:
			pos.x = playFieldCenter.x + playFieldOffset.x;
            break;
	}
	switch (y) {
		case 0:
			pos.y = playFieldCenter.y + playFieldOffset.y + scale;
			break;
	}
	return pos;
}

configure(new Effect());

function LutSlider() {
	var self = this;
	this.lutLeft = 0;
	this.lutRight = 1;
	this.lutSliderPos = 0.0;

	this.swipeTargetPos = null;
	this.lastAppliedLut = null;

	this.setLutSlider = function (value) {
		Api.meshfxMsg("shaderVec4", 0, 4, String(value));
	};

	this.applyLutSliderTex = function () {
		Api.meshfxMsg("tex", 20, 0, effectData.final_luts[self.lutRight]);
		Api.meshfxMsg("tex", 20, 1, effectData.final_luts[self.lutLeft]);
	};

	this.invertLuts = function (direction) { // 1 - right, other - left
		var _lut;
		if (direction == 1) {
			_lut = self.lutRight >= effectData.final_luts.length - 1 ? 0 : self.lutRight + 1;
			self.lutLeft = self.lutRight;
			self.lutRight = _lut;
		} else {
			_lut = self.lutLeft <= 0 ? effectData.final_luts.length - 1 : self.lutLeft - 1;
			self.lutRight = self.lutLeft;
			self.lutLeft = _lut;
		}

		self.applyLutSliderTex();
	};

	this.update = function () {
		if (self.swipeTargetPos != null) {
			self.lutSliderPos = smoothMove(self.lutSliderPos, self.swipeTargetPos);
			if (Math.abs(self.lutSliderPos - self.swipeTargetPos) <= 0.01) {
				self.lutSliderPos = self.swipeTargetPos;
				var appliedLut = self.swipeTargetPos == 1 ? self.lutLeft : self.lutRight;
				if (appliedLut != self.lastAppliedLut) self.lutAppliedCallback(effectData.final_luts[appliedLut]);
				self.lastAppliedLut = appliedLut;
				self.swipeTargetPos = null;
			}
		}
		self.setLutSlider(self.lutSliderPos);
	};

	onTouchesBeganActions.push(function (touches) {
		self.swipeTargetPos = null;
	});

	onTouchesMovedActions.push(function (touches) {
		self.lutSliderPos += (touches[0].x - lastFingerPosX) * 0.5;
		if (self.lutSliderPos > 1) {
			self.lutSliderPos = self.lutSliderPos - 1;
			self.invertLuts(0);
		}
		if (self.lutSliderPos < 0) {
			self.lutSliderPos = 1 + self.lutSliderPos;
			self.invertLuts(1);
		}
	});

	onTouchesEndedActions.push(function (touches) {
		if (fingerVelocity > 0) {
			self.swipeTargetPos = 1.0;
			analytic.swipes++;
			return;
		}
		if (fingerVelocity < 0) {
			self.swipeTargetPos = 0.0;
			analytic.swipes++;
			return;
		}

		if (self.lutSliderPos > 0.5) {
			self.swipeTargetPos = 1.0;
			analytic.swipes++;
		} else {
			self.swipeTargetPos = 0.0;
			analytic.swipes++;
		}
		
	});

	this.lutAppliedCallback = function (lutName) {
		var data = {
			"lut_name": lutName
		};

		try {
			Api.effectEvent("lut_applied", JSON.stringify(data));
			Api.print("Event: lut_applied, data: " + JSON.stringify(data));
		} catch (err) {
			Api.print(err);
		}
	};

	Api.meshfxMsg("spawn", 20, 0, "lut_swipe.bsm2");
	this.setLutSlider(0.0);
	this.applyLutSliderTex();

	effect.faceActions.push(self.update);
	effect.noFaceActions.push(self.update);
}

function timeOut(delay, callback) {
    var timer = new Date().getTime();

    effect.faceActions.push(removeAfterTimeOut);
    effect.noFaceActions.push(removeAfterTimeOut);

    function removeAfterTimeOut() {
        var now = new Date().getTime();

        if (now >= timer + delay) {
            var idx = effect.faceActions.indexOf(removeAfterTimeOut);
            effect.faceActions.splice(idx, 1);
            idx = effect.noFaceActions.indexOf(removeAfterTimeOut);
            effect.noFaceActions.splice(idx, 1);
            callback();
        }
    }
}

var slider = new LutSlider();
