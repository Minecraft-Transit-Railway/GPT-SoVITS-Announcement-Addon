import {Component} from "@angular/core";
import {ButtonModule} from "primeng/button";
import {FloatLabelModule} from "primeng/floatlabel";
import {CardModule} from "primeng/card";
import {FormControl, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {HttpClient} from "@angular/common/http";
import {url} from "../../utility/settings";
import {ControlWithStatusComponent} from "../control-with-status/control-with-status.component";
import {TextareaModule} from "primeng/textarea";
import {TextInputBaseComponent} from "../text-input-base/text-input-base.component";
import {PlayerTracking} from "../../data/playerTracking";

@Component({
	selector: "app-player-tracking-input",
	imports: [
		CardModule,
		FloatLabelModule,
		TextareaModule,
		ButtonModule,
		ReactiveFormsModule,
		ControlWithStatusComponent,
	],
	templateUrl: "./player-tracking-input.component.html",
	styleUrl: "./player-tracking-input.component.css",
})
export class PlayerTrackingInputComponent extends TextInputBaseComponent<PlayerTracking> {
	protected readonly formGroup = new FormGroup({
		playerTracking: new FormControl(""),
	});
	private loading = false;
	private status = "";

	constructor(private readonly httpClient: HttpClient) {
		super(data => new PlayerTracking(data));
	}

	update() {
		const playerTracking = this.serialize();
		if (playerTracking) {
			this.loading = true;
			this.status = "Updating player tracking";
			this.httpClient.post<boolean>(`${url}/api/setPlayerTracking`, playerTracking).subscribe({
				next: success => {
					this.loading = false;
					this.status = success ? "Player tracking updated" : "Player tracking failed";
				},
				error: () => {
					this.loading = false;
					this.status = "Player tracking failed";
				},
			});
		}
	}

	isLoading() {
		return this.loading;
	}

	getStatus() {
		return this.status;
	}

	serialize() {
		const playerTrackingList = this.serializeRaw(this.formGroup.get("playerTracking"));
		return playerTrackingList.length === 1 ? playerTrackingList[0] : undefined;
	}
}
