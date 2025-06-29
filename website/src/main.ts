import "reflect-metadata";
import {bootstrapApplication} from "@angular/platform-browser";
import {AppComponent} from "./app/app.component";
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {provideHttpClient} from "@angular/common/http";
import {isDevMode} from "@angular/core";
import {providePrimeNG} from "primeng/config";
import {myPreset} from "./theme-preset";
import {provideTransloco} from "@jsverse/transloco";
import {TranslocoHttpLoader} from "./transloco-loader";

bootstrapApplication(AppComponent, {
	providers: [
		provideAnimationsAsync(),
		provideHttpClient(),
		providePrimeNG({
			theme: {
				preset: myPreset,
				options: {darkModeSelector: ".dark-theme"},
			},
		}),
		provideTransloco({
			config: {
				availableLangs: ["en", "zh"],
				defaultLang: "en",
				reRenderOnLangChange: true,
				prodMode: !isDevMode(),
			},
			loader: TranslocoHttpLoader,
		}),
	],
}).catch(error => console.error(error));
