import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { competitorsApi } from "../../api/competitors";
import type { CompetitorType } from "../../api/types";
import { ApiError } from "../../api/apiClient";
import { COMPETITOR_AVATAR } from "../../assets/competitorAvatars";

type FormState = {
    name: string;
    nickname: string;
    competitorType: CompetitorType;
    dateOfBirth: string;
    approximateAge: string;
    height: string;
    weight: string;
    origin: string;
};

const EMPTY_FORM: FormState = {
    name: "",
    nickname: "",
    competitorType: "DWARF",
    dateOfBirth: "",
    approximateAge: "",
    height: "",
    weight: "",
    origin: "",
};

/** Strips everything except digits and (optionally) a single decimal point,
 * so typing/pasting can never produce a negative number or stray characters.
 * allowDecimal=false is used for approximateAge, which should stay a whole number. */
function sanitizeNumeric(value: string, allowDecimal: boolean): string {
    let cleaned = allowDecimal ? value.replace(/[^0-9.]/g, "") : value.replace(/[^0-9]/g, "");
    if (allowDecimal) {
        const firstDot = cleaned.indexOf(".");
        if (firstDot !== -1) {
            cleaned = cleaned.slice(0, firstDot + 1) + cleaned.slice(firstDot + 1).replace(/\./g, "");
        }
    }
    return cleaned;
}

export function CompetitorCreatePage() {
    const navigate = useNavigate();

    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
    const [formError, setFormError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
        setForm((f) => ({ ...f, [key]: value }));
        if (fieldErrors[key]) {
            setFieldErrors((errs) => {
                const next = { ...errs };
                delete next[key];
                return next;
            });
        }
    }

    function updateNumericField(
        key: "approximateAge" | "height" | "weight",
        value: string,
        allowDecimal: boolean
    ) {
        updateField(key, sanitizeNumeric(value, allowDecimal));
    }

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setSaving(true);
        setFormError(null);
        setFieldErrors({});

        try {
            if (!form.dateOfBirth && !form.approximateAge) {
                setFieldErrors({ object: "Provide a date of birth or an approximate age." });
                setSaving(false);
                return;
            }

            const created = await competitorsApi.create({
                name: form.name,
                nickname: form.nickname,
                competitorType: form.competitorType,
                dateOfBirth: form.dateOfBirth || undefined,
                approximateAge: form.approximateAge ? Number(form.approximateAge) : undefined,
                height: Number(form.height),
                weight: Number(form.weight),
                origin: form.origin,
            });
            navigate(`/competitors/${created.id}`, { state: { justCreated: true } });
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.body?.validationErrors) {
                    setFieldErrors(err.body.validationErrors);
                } else if (err.status === 409) {
                    setFormError(err.body?.message ?? "This competitor conflicts with an existing one.");
                } else if (err.status === 400) {
                    setFormError(err.body?.message ?? "The request couldn't be processed.");
                } else if (err.status === 403) {
                    setFormError("You do not have permission to create a competitor.");
                } else {
                    setFormError(err.body?.message ?? "Something went wrong creating this competitor.");
                }
            } else {
                setFormError("Something went wrong creating this competitor.");
            }
        } finally {
            setSaving(false);
        }
    }

    return (
        <div>
            <Link to="/competitors" className="back-link">
                ← Back to competitors
            </Link>

            <div className="form-header">
                <img src={COMPETITOR_AVATAR[form.competitorType]} alt="" className="form-header__avatar" />
                <div>
                    <h1>New competitor</h1>
                    <p className="app-content__subtitle" style={{ marginBottom: 0 }}>
                        Register a racer to add them to the roster.
                    </p>
                </div>
            </div>

            {formError && (
                <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                    <p>{formError}</p>
                </div>
            )}
            {fieldErrors.object && (
                <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                    <p>{fieldErrors.object}</p>
                </div>
            )}

            <form onSubmit={handleSubmit} className="detail-card form-card">
                <div className="form-section">
                    <h3>Identity</h3>
                    <div className="form-grid">
                        <div className="form-field">
                            <label>Name</label>
                            <input value={form.name} onChange={(e) => updateField("name", e.target.value)} />
                            {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
                        </div>
                        <div className="form-field">
                            <label>Nickname</label>
                            <input value={form.nickname} onChange={(e) => updateField("nickname", e.target.value)} />
                            {fieldErrors.nickname && <span className="field-error">{fieldErrors.nickname}</span>}
                        </div>
                        <div className="form-field">
                            <label>Type</label>
                            <select
                                value={form.competitorType}
                                onChange={(e) => updateField("competitorType", e.target.value as CompetitorType)}
                            >
                                <option value="CAMEL">Camel</option>
                                <option value="DWARF">Dwarf</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="OTHER">Other</option>
                            </select>
                            {fieldErrors.competitorType && (
                                <span className="field-error">{fieldErrors.competitorType}</span>
                            )}
                        </div>
                        <div className="form-field">
                            <label>Origin</label>
                            <input value={form.origin} onChange={(e) => updateField("origin", e.target.value)} />
                            {fieldErrors.origin && <span className="field-error">{fieldErrors.origin}</span>}
                        </div>
                    </div>
                </div>

                <div className="form-section">
                    <h3>Vital stats</h3>
                    <div className="form-grid">
                        <div className="form-field">
                            <label>Date of birth</label>
                            <input
                                type="date"
                                value={form.dateOfBirth}
                                onChange={(e) => updateField("dateOfBirth", e.target.value)}
                            />
                            {fieldErrors.dateOfBirth && <span className="field-error">{fieldErrors.dateOfBirth}</span>}
                        </div>
                        <div className="form-field">
                            <label>Approximate age (if no date of birth)</label>
                            <input
                                type="text"
                                inputMode="numeric"
                                value={form.approximateAge}
                                onChange={(e) => updateNumericField("approximateAge", e.target.value, false)}
                            />
                            {fieldErrors.approximateAge && (
                                <span className="field-error">{fieldErrors.approximateAge}</span>
                            )}
                        </div>
                        <div className="form-field">
                            <label>Height (cm)</label>
                            <input
                                type="text"
                                inputMode="decimal"
                                value={form.height}
                                onChange={(e) => updateNumericField("height", e.target.value, true)}
                            />
                            {fieldErrors.height && <span className="field-error">{fieldErrors.height}</span>}
                        </div>
                        <div className="form-field">
                            <label>Weight (kg)</label>
                            <input
                                type="text"
                                inputMode="decimal"
                                value={form.weight}
                                onChange={(e) => updateNumericField("weight", e.target.value, true)}
                            />
                            {fieldErrors.weight && <span className="field-error">{fieldErrors.weight}</span>}
                        </div>
                    </div>
                </div>

                <div className="form-actions">
                    <button type="submit" className="btn btn-primary" disabled={saving}>
                        {saving ? "Creating..." : "Create competitor"}
                    </button>
                    <Link to="/competitors" className="btn btn-secondary">
                        Cancel
                    </Link>
                </div>
            </form>
        </div>
    );
}