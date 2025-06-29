import {ValidatedDataBase} from "./validatedDataBase";

export class SynthesisRequest extends ValidatedDataBase {
	public readonly voiceId: string;
	public readonly text: string;

	constructor(synthesisRequest: SynthesisRequest) {
		super();
		this.voiceId = synthesisRequest.voiceId;
		this.text = synthesisRequest.text;
	}

	public isValid() {
		return !!this.voiceId && !!this.text;
	}
}
