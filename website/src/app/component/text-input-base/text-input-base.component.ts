import {ValidatedDataBase} from "../../data/validatedDataBase";

export abstract class TextInputBaseComponent<T extends ValidatedDataBase> {

	protected constructor(private readonly copyDataInstance: (data: T) => T) {
	}

	protected serializeRaw(input: string | null) {
		if (input) {
			try {
				return (JSON.parse(input) as T[]).map(data => this.copyDataInstance(data)).filter(data => data.isValid());
			} catch (e) {
				console.debug(e);
			}

			try {
				return [this.copyDataInstance(JSON.parse(input) as T)].filter(data => data.isValid());
			} catch (e) {
				console.debug(e);
			}
		}

		return [];
	}
}
