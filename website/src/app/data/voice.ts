import {ValidatedDataBase} from "./validatedDataBase";

export class Voice extends ValidatedDataBase {
	public readonly id: string;
	public readonly runtimeCode: string;
	public readonly ckptPath: string;
	public readonly pthPath: string;
	public readonly voiceSamplePath: string;
	public readonly voiceSampleText: string;

	constructor(voice: Voice) {
		super();
		this.id = voice.id;
		this.runtimeCode = voice.runtimeCode;
		this.ckptPath = voice.ckptPath;
		this.pthPath = voice.pthPath;
		this.voiceSamplePath = voice.voiceSamplePath;
		this.voiceSampleText = voice.voiceSampleText;
	}

	public isValid() {
		return !!this.id && !!this.runtimeCode && !!this.ckptPath && !!this.pthPath && !!this.voiceSamplePath && !!this.voiceSampleText;
	}
}
