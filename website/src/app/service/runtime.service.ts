import {EventEmitter, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {url} from "../utility/settings";

@Injectable({providedIn: "root"})
export class RuntimeService {
	public readonly stateChanged = new EventEmitter<boolean>();
	private isRunning?: boolean;

	constructor(private readonly httpClient: HttpClient) {
		const updateStatus = () => httpClient.get<boolean>(`${url}/api/isRunning`).subscribe(isRunning => {
			if (isRunning !== this.isRunning) {
				this.stateChanged.emit(isRunning);
			}
			this.isRunning = isRunning;
		});
		updateStatus();
		setInterval(updateStatus, 1000);
	}

	public start() {
		this.httpClient.get<boolean>(`${url}/api/start`).subscribe();
	}

	public stop() {
		this.httpClient.get<boolean>(`${url}/api/stop`).subscribe();
	}
}
