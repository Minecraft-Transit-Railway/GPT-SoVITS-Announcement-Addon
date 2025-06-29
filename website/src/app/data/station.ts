import {Route} from "./route";

export class Station {
	public readonly id: string = "";
	public readonly name: string = "";
	public readonly connections: Station[] = [];
	public readonly routes: Route[] = [];
}
