function Effect() {
    var self = this;

    this.init = function() {
        if(Api.getPlatform() == "ios" || Api.getPlatform() == "iOS" || Api.getPlatform() == "macOS") {
            Api.meshfxMsg("spawn", 2, 0, "tri.bsm2");
            Api.meshfxMsg("spawn", 3, 0, "tri2.bsm2");
            var resolution;
		    // Api.meshfxMsg( "shaderVec4", 0, 1, resolution + " 0.0 0.0 0.0");
            if (Api.getPlatform().toLowerCase != 'macos') {
                resolution = Api.visibleAreaWidth();
            } else {
                resolution = 720;
            }
            Api.meshfxMsg( "shaderVec4", 0, 2, resolution + " 0.0 0.0 0.0");
            Api.meshfxMsg( "shaderVec4", 0, 3, resolution + " 0.0 0.0 0.0");
        }
            

        Api.meshfxMsg("spawn", 1, 0, "!glfx_FACE");

        Api.meshfxMsg("spawn", 0, 0, "Beauty09.bsm2");
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