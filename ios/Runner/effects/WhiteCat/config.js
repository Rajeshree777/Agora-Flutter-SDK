
function Effect() {
	var self = this;

	this.init = function() {
		Api.meshfxMsg("spawn", 1, 0, "!glfx_FACE");
		Api.meshfxMsg("spawn", 0, 0, "cat_v2.bsm2");
		Api.playSound("caaat_sndfnt_v2_L_Channel.ogg", true, 1);
		Api.playVideo("frx",true,1);
		Api.showRecordButton();
	};

	this.restart = function() {
		Api.meshfxReset();
		self.init();
	};

	this.faceActions = [];
	this.noFaceActions = [];

	this.videoRecordStartActions = [];
	this.videoRecordFinishActions = [];
	this.videoRecordDiscardActions = [this.restart];
}

configure(new Effect());
