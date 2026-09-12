import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { teamsApi } from "../../api/teams";
import { ApiError } from "../../api/apiClient";
import dwarfBg from "../../assets/dwarf.jpg";

type FormState = {
    name: string;
    description: string;
    coach: string;
    maxMembers: string;
};

const EMPTY_FORM: FormState = {
    name: "",
    description: "",
    coach: "",
    maxMembers: "",
};

function sanitizeWholeNumber(value: string): string {
    return value.replace(/[^0-9]/g, "");
}

export function TeamCreatePage() {
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

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setSaving(true);
        setFormError(null);
        setFieldErrors({});

        try {
            if (!form.maxMembers) {
                setFieldErrors({ maxMembers: "Set a maximum number of members." });
                setSaving(false);
                return;
            }

            const created = await teamsApi.create({
                name: form.name,
                description: form.description || undefined,
                coach: form.coach,
                maxMembers: Number(form.maxMembers),
            });
            navigate(`/teams/${created.id}`, { state: { justCreated: true } });
        } catch (err) {
            if (err instanceof ApiError) {
                if (err.body?.validationErrors) {
                    setFieldErrors(err.body.validationErrors);
                } else if (err.status === 409) {
                    setFormError(err.body?.message ?? "This team conflicts with an existing one.");
                } else if (err.status === 400) {
                    setFormError(err.body?.message ?? "The request couldn't be processed.");
                } else if (err.status === 403) {
                    setFormError("You do not have permission to create a team.");
                } else {
                    setFormError(err.body?.message ?? "Something went wrong creating this team.");
                }
            } else {
                setFormError("Something went wrong creating this team.");
            }
        } finally {
            setSaving(false);
        }
    }

    return (
        <>
            <div className="page-bg-fixed" style={{ backgroundImage: `url(${dwarfBg})` }} />
            <div>
                <Link to="/teams" className="page-breadcrumb">
                    ← Back to teams
                </Link>

                <h1 className="page-title-banner page-title-banner--teams">New team</h1>
                <p className="app-content__subtitle" style={{ textAlign: "center" }}>
                    Register a team so competitors can be assigned to it.
                </p>

                {formError && (
                    <div className="state-box state-box--error" style={{ marginBottom: 16 }}>
                        <p>{formError}</p>
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
                                <label>Coach</label>
                                <input value={form.coach} onChange={(e) => updateField("coach", e.target.value)} />
                                {fieldErrors.coach && <span className="field-error">{fieldErrors.coach}</span>}
                            </div>
                            <div className="form-field">
                                <label>Max members</label>
                                <input
                                    type="text"
                                    inputMode="numeric"
                                    value={form.maxMembers}
                                    onChange={(e) => updateField("maxMembers", sanitizeWholeNumber(e.target.value))}
                                />
                                {fieldErrors.maxMembers && <span className="field-error">{fieldErrors.maxMembers}</span>}
                            </div>
                            <div className="form-field" style={{ gridColumn: "1 / -1" }}>
                                <label>Description</label>
                                <textarea
                                    rows={2}
                                    value={form.description}
                                    onChange={(e) => updateField("description", e.target.value)}
                                />
                                {fieldErrors.description && (
                                    <span className="field-error">{fieldErrors.description}</span>
                                )}
                            </div>
                        </div>
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Creating..." : "Create team"}
                        </button>
                        <Link to="/teams" className="btn btn-secondary">
                            Cancel
                        </Link>
                    </div>
                </form>
            </div>
        </>
    );
}