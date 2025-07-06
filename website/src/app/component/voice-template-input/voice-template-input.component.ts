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
import {DialogModule} from "primeng/dialog";
import {ControlWithStatusComponent} from "../control-with-status/control-with-status.component";
import {ProgressSpinnerModule} from "primeng/progressspinner";

@Component({
	selector: "app-voice-template-input",
	imports: [
		CardModule,
		FloatLabelModule,
		TextareaModule,
		ButtonModule,
		DialogModule,
		ProgressSpinnerModule,
		ReactiveFormsModule,
		ControlWithStatusComponent,
	],
	templateUrl: "./voice-template-input.component.html",
	styleUrl: "./voice-template-input.component.css",
})
export class VoiceTemplateInputComponent extends TextInputBaseComponent<Voice> {
	protected readonly formGroup = new FormGroup({
		voiceTemplates: new FormControl(""),
	});
	protected readonly voiceIds: string[] = [];
	protected voicesDialogVisible = false;
	private addVoicesLoading = false;
	private voicesDialogLoading = false;
	private status = "";

	constructor(private readonly httpClient: HttpClient) {
		super(data => new Voice(data));
	}

	add() {
		this.addVoicesLoading = true;
		this.status = "Adding voices...";
		const successfulVoices: string[] = [];
		const failedVoices: string[] = [];
		const voices = this.serialize();
		const finish = () => {
			if (successfulVoices.length + failedVoices.length === voices.length) {
				this.addVoicesLoading = false;
				if (failedVoices.length == 0) {
					this.status = `${successfulVoices.length} voice(s) added`;
				} else if (successfulVoices.length == 0) {
					this.status = `Failed to add ${failedVoices.length} voice(s)`;
				} else {
					this.status = `${successfulVoices.length} voice(s) added, failed to add ${failedVoices.length} voice(s)`;
				}
			}
		};

		voices.forEach(voice => {
			const newUrl = `${url}/api/addVoice?id=${encodeURIComponent(voice.id)}&runtimeCode=${encodeURIComponent(voice.runtimeCode)}&ckptPath=${encodeURIComponent(voice.ckptPath)}&pthPath=${encodeURIComponent(voice.pthPath)}&voiceSamplePath=${encodeURIComponent(voice.voiceSamplePath)}&voiceSampleText=${encodeURIComponent(voice.voiceSampleText)}&retries=5`;
			this.httpClient.get<boolean>(newUrl).subscribe({
				next: result => {
					(result ? successfulVoices : failedVoices).push(voice.id);
					finish();
				},
				error: () => {
					failedVoices.push(voice.id);
					finish();
				},
			});
		});
	}

	isAddVoicesLoading() {
		return this.addVoicesLoading;
	}

	isVoiceDialogLoading() {
		return this.voicesDialogLoading;
	}

	getStatus() {
		return this.status;
	}

	serialize() {
		return this.serializeRaw(this.formGroup.get("voiceTemplates"));
	}

	showVoicesDialog() {
		this.voicesDialogVisible = true;
		this.voicesDialogLoading = true;
		this.voiceIds.length = 0;
		this.httpClient.get<string[]>(`${url}/api/voices`).subscribe({
			next: voiceIds => {
				this.voicesDialogLoading = false;
				this.voiceIds.push(...voiceIds);
			},
			error: () => this.voicesDialogLoading = false,
		});
	}
}
