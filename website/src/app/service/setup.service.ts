import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {url} from "../utility/settings";

@Injectable({providedIn: "root"})
export class SetupService {
	private version = "";

	constructor(private readonly httpClient: HttpClient) {
		this.updateVersion();
	}

	public prepare(callback: (success: boolean) => void) {
		this.httpClient.get<boolean>(`${url}/api/prepare`).subscribe({
			next: success => {
				this.updateVersion();
				callback(success);
			}, error: () => {
				this.updateVersion();
				callback(false);
			},
		});
	}

	public download(callback: (success: boolean) => void) {
		this.httpClient.get<boolean>(`${url}/api/download?retries=100`).subscribe({
			next: success => {
				this.updateVersion();
				callback(success);
			}, error: () => {
				this.updateVersion();
				callback(false);
			},
		});
	}

	public unzip(callback: (success: boolean) => void) {
		this.httpClient.get<boolean>(`${url}/api/unzip?retries=100`).subscribe({
			next: success => {
				this.updateVersion();
				callback(success);
			}, error: () => {
				this.updateVersion();
				callback(false);
			},
		});
	}

	public finish(callback: (success: boolean) => void) {
		this.httpClient.get<boolean>(`${url}/api/finish`).subscribe({
			next: success => {
				this.updateVersion();
				callback(success);
			}, error: () => {
				this.updateVersion();
				callback(false);
			},
		});
	}

	public getVersion() {
		return this.version ? this.version : undefined;
	}

	private updateVersion() {
		this.httpClient.get(`${url}/api/version`, {responseType: "text"}).subscribe(version => this.version = version);
	}
}
