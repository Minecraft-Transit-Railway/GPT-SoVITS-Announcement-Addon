import {Component} from "@angular/core";
import {PlayerTrackingInputComponent} from "./component/player-tracking-input/player-tracking-input.component";
import {VoiceTemplateInputComponent} from "./component/voice-template-input/voice-template-input.component";
import {RuntimeControlComponent} from "./component/runtime-control/runtime-control.component";
import {SynthesisRequestInputComponent} from "./component/test-input/synthesis-request-input.component";

@Component({
	selector: "app-root",
	imports: [
		RuntimeControlComponent,
		VoiceTemplateInputComponent,
		SynthesisRequestInputComponent,
		PlayerTrackingInputComponent,
	],
	templateUrl: "./app.component.html",
	styleUrls: ["./app.component.css"],
})
export class AppComponent {
}
