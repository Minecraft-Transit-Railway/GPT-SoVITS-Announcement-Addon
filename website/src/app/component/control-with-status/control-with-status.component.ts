import {Component, Input} from "@angular/core";
import {ProgressSpinnerModule} from "primeng/progressspinner";

@Component({
	selector: "app-control-with-status",
	imports: [
		ProgressSpinnerModule,
	],
	templateUrl: "./control-with-status.component.html",
	styleUrl: "./control-with-status.component.css",
})
export class ControlWithStatusComponent {
	@Input({required: true}) loading = false;
	@Input({required: true}) status = "";
}
