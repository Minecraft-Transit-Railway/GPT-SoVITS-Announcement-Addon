import {ValidatedDataBase} from "../../data/validatedDataBase";
import {AbstractControl} from "@angular/forms";

export abstract class TextInputBaseComponent<T extends ValidatedDataBase> {

	protected constructor(private readonly copyDataInstance: (data: T) => T) {
	}

	protected serializeRaw(input: AbstractControl<string | null, string | null> | null) {
		if (input) {
			input.setErrors(null);

			try {
				const rawString = input.getRawValue();
				const data = JSON.parse(rawString);
				const prettyString = JSON.stringify(data, null, 4);

				if (rawString !== prettyString) {
					input.setValue(prettyString);
				}

				try {
					return (data as T[]).map(data => this.copyDataInstance(data)).filter(data => data.isValid());
				} catch (e) {
					console.debug(e);
				}

				try {
					return [this.copyDataInstance(data as T)].filter(data => data.isValid());
				} catch (e) {
					console.debug(e);
				}
			} catch (e) {
				console.debug(e);
			}

			input.setErrors({error: ""});
		}

		return [];
	}
}
