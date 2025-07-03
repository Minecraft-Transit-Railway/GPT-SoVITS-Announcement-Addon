import {Component} from "@angular/core";
import {CardModule} from "primeng/card";
import {FormControl, FormGroup, FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ToggleSwitchModule} from "primeng/toggleswitch";
import {SetupService} from "../../service/setup.service";
import {SplitButtonModule} from "primeng/splitbutton";
import {MenuItem} from "primeng/api";
import {ButtonModule} from "primeng/button";
import {ProgressSpinnerModule} from "primeng/progressspinner";
import {ControlWithStatusComponent} from "../control-with-status/control-with-status.component";

@Component({
	selector: "app-runtime-control-input",
	imports: [
		CardModule,
		ButtonModule,
		SplitButtonModule,
		ToggleSwitchModule,
		ProgressSpinnerModule,
		ReactiveFormsModule,
		FormsModule,
		ControlWithStatusComponent,
	],
	templateUrl: "./runtime-control.component.html",
	styleUrl: "./runtime-control.component.css",
})
export class RuntimeControlComponent {
	protected readonly reinstallOptions: MenuItem[];
	protected readonly formGroup = new FormGroup({
		runtimeToggle: new FormControl(false),
	});
	private loading = false;
	private status = "";

	constructor(private readonly setupService: SetupService) {
		this.reinstallOptions = [
			{
				label: "Prepare Files",
				command: () => this.prepare(),
			},
			{
				label: "Download GPT-SoVITS",
				command: () => this.download(),
			},
			{
				label: "Unzip GPT-SoVITS",
				command: () => this.unzip(),
			},
			{
				label: "Cleanup Files",
				command: () => this.finish(),
			},
		];
	}

	install() {
		this.prepare(() => this.download(() => this.unzip(() => this.finish())));
	}

	isLoading() {
		return this.loading;
	}

	getStatus() {
		return this.status;
	}

	getVersion() {
		return this.setupService.getVersion();
	}

	private prepare(callback?: () => void) {
		this.runTask("Preparing files...", "Prepared files", "Failed to prepare files!", (innerCallback) => this.setupService.prepare(innerCallback), callback);
	}

	private download(callback?: () => void) {
		this.runTask("Downloading GPT-SoVITS...", "Downloaded GPT-SoVITS", "Failed to download GPT-SoVITS!", (innerCallback) => this.setupService.download(innerCallback), callback);
	}

	private unzip(callback?: () => void) {
		this.runTask("Unzipping GPT-SoVITS...", "Unzipped GPT-SoVITS", "Failed to unzip GPT-SoVITS!", (innerCallback) => this.setupService.unzip(innerCallback), callback);
	}

	private finish(callback?: () => void) {
		this.runTask("Cleaning up...", "Cleanup finished", "Failed to clean up!", (innerCallback) => this.setupService.finish(innerCallback), callback);
	}

	private runTask(startStatus: string, finishedStatus: string, failedStatus: string, task: (callback: (success: boolean) => void) => void, callback?: () => void) {
		this.status = startStatus;
		this.loading = true;
		task(success => {
			this.loading = false;
			if (success) {
				this.status = finishedStatus;
				if (callback) {
					callback();
				}
			} else {
				this.status = failedStatus;
			}
		});
	}
}
