import {Component} from "@angular/core";
import {ButtonModule} from "primeng/button";
import {FloatLabelModule} from "primeng/floatlabel";
import {CardModule} from "primeng/card";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TextareaModule} from "primeng/textarea";
import {Voice} from "../../data/voice";
import {HttpClient} from "@angular/common/http";
import {url} from "../../utility/settings";
import {TextInputBaseComponent} from "../text-input-base/text-input-base.component";

@Component({
	selector: "app-voice-template-input",
	imports: [
		CardModule,
		FloatLabelModule,
		TextareaModule,
		ButtonModule,
		ReactiveFormsModule,
	],
	templateUrl: "./voice-template-input.component.html",
	styleUrl: "./voice-template-input.component.css",
})
export class VoiceTemplateInputComponent extends TextInputBaseComponent<Voice> {
	protected readonly formGroup = new FormGroup({
		voiceTemplates: new FormControl(""),
	});
	private status = "";

	constructor(private readonly httpClient: HttpClient) {
		super(data => new Voice(data));
	}

	add() {
		const addedVoices: string[] = [];
		const replacedVoices: string[] = [];
		this.serialize().forEach(voice => {
			const newUrl = `${url}/api/addVoice?id=${encodeURIComponent(voice.id)}&runtimeCode=${encodeURIComponent(voice.runtimeCode)}&ckptPath=${encodeURIComponent(voice.ckptPath)}&pthPath=${encodeURIComponent(voice.pthPath)}&voiceSamplePath=${encodeURIComponent(voice.voiceSamplePath)}&voiceSampleText=${encodeURIComponent(voice.voiceSampleText)}`;
			this.httpClient.get<boolean>(newUrl).subscribe(result => {
				(result ? addedVoices : replacedVoices).push(voice.id);
				this.status = `Added ${addedVoices.length} voice(s), replaced ${replacedVoices.length} voice(s)`;
			});
		});
	}

	getStatus() {
		return this.status;
	}

	serialize() {
		return this.serializeRaw(this.formGroup.getRawValue().voiceTemplates);
	}
}
