import {Station} from "./station";
import {Route} from "./route";

export class Client {
	public readonly id: string = "";
	public readonly atStation: boolean = false;
	public readonly route?: Route;
	public readonly station1?: Station;
	public readonly station2?: Station;
}
