import {Component, OnInit} from "@angular/core";
import {ButtonModule} from "primeng/button";
import {InputTextModule} from "primeng/inputtext";
import {FloatLabelModule} from "primeng/floatlabel";
import {CardModule} from "primeng/card";
import {CheckboxModule} from "primeng/checkbox";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {InputNumberModule} from "primeng/inputnumber";
import {ServerService} from "../../service/server.service";

@Component({
	selector: "app-player-tracking-input",
	imports: [
		CardModule,
		CheckboxModule,
		FloatLabelModule,
		InputTextModule,
		InputNumberModule,
		ButtonModule,
		ReactiveFormsModule,
	],
	templateUrl: "./player-tracking-input.component.html",
	styleUrl: "./player-tracking-input.component.css",
})
export class PlayerTrackingInputComponent implements OnInit {
	protected readonly formGroup = new FormGroup({
		trackingEnabled: new FormControl(true),
		serverUrl: new FormControl(""),
		trackingDimension: new FormControl(0),
		trackingPlayer: new FormControl(""),
	});

	constructor(private readonly serverService: ServerService) {
	}

	ngOnInit() {
		this.formGroup.get("trackingEnabled")?.valueChanges.subscribe(enabled => {
			if (enabled) {
				this.formGroup.get("serverUrl")?.enable();
				this.formGroup.get("trackingPlayer")?.enable();
			} else {
				this.formGroup.get("serverUrl")?.disable();
				this.formGroup.get("trackingPlayer")?.disable();
			}
		});
	}

	updateDisabled() {
		const data = this.formGroup.getRawValue();
		return !data.trackingEnabled || !data.serverUrl || data.trackingDimension === null || !data.trackingPlayer;
	}

	update() {
		const data = this.formGroup.getRawValue();
		if (data.serverUrl && data.trackingDimension !== null && data.trackingPlayer) {
			this.serverService.updateServer(data.serverUrl, data.trackingDimension, data.trackingPlayer);
		}
	}
}
