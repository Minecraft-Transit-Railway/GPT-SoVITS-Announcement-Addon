import {Component} from "@angular/core";
import {ButtonModule} from "primeng/button";
import {FloatLabelModule} from "primeng/floatlabel";
import {CardModule} from "primeng/card";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {TextareaModule} from "primeng/textarea";
import {HttpClient} from "@angular/common/http";
import {url} from "../../utility/settings";
import {SynthesisRequest} from "../../data/synthesisRequest";
import {TextInputBaseComponent} from "../text-input-base/text-input-base.component";
import {ControlWithStatusComponent} from "../control-with-status/control-with-status.component";

@Component({
	selector: "app-synthesis-request-input",
	imports: [
		CardModule,
		FloatLabelModule,
		TextareaModule,
		ButtonModule,
		ReactiveFormsModule,
		ControlWithStatusComponent,
	],
	templateUrl: "./synthesis-request-input.component.html",
	styleUrl: "./synthesis-request-input.component.css",
})
export class SynthesisRequestInputComponent extends TextInputBaseComponent<SynthesisRequest> {
	protected readonly formGroup = new FormGroup({
		synthesisRequests: new FormControl(""),
	});
	private loading = false;
	private status = "";

	constructor(private readonly httpClient: HttpClient) {
		super(data => new SynthesisRequest(data));
	}

	submit() {
		const synthesisRequests = this.serialize();
		if (synthesisRequests.length > 0) {
			this.loading = true;
			this.status = "Text to speech synthesis in progress...";

			this.httpClient.get<string[]>(`${url}/api/voices`).subscribe({
				next: voiceIds => {
					const missingVoiceIds: string[] = [];
					synthesisRequests.forEach(synthesisRequest => {
						if (!voiceIds.includes(synthesisRequest.voiceId)) {
							missingVoiceIds.push(synthesisRequest.voiceId);
						}
					});

					if (missingVoiceIds.length === 0) {
						this.httpClient.post<boolean>(`${url}/api/synthesize?key=test&retries=2`, synthesisRequests).subscribe({
							next: success => {
								this.loading = false;
								this.status = success ? "Text to speech synthesis successful" : "Text to speech synthesis failed";
							},
							error: () => {
								this.loading = false;
								this.status = "Text to speech synthesis failed";
							},
						});
					} else {
						this.loading = false;
						this.status = `${missingVoiceIds.length} missing voice(s): ${missingVoiceIds.join(", ")}`;
					}
				},
				error: () => {
					this.loading = false;
					this.status = "Text to speech synthesis failed";
				},
			});
		}
	}

	play() {
		this.httpClient.get<boolean>(`${url}/api/play?key=test`).subscribe({
			next: success => {
				if (!success) {
					this.status = "Nothing to play";
				}
			},
			error: () => {
				this.status = "Play failed";
			},
		});
	}

	isLoading() {
		return this.loading;
	}

	getStatus() {
		return this.status;
	}

	serialize() {
		return this.serializeRaw(this.formGroup.get("synthesisRequests"));
	}
}
