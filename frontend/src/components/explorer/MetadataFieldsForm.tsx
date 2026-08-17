import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { metadataApi, MetadataDefinition } from '../../api/metadataApi';
import { DocumentMetadataValue } from '../../types';
import { Sliders } from 'lucide-react';

interface MetadataFieldsFormProps {
  categoryId?: string;
  values: DocumentMetadataValue[];
  onChange: (values: DocumentMetadataValue[]) => void;
}

export const MetadataFieldsForm: React.FC<MetadataFieldsFormProps> = ({ categoryId, values, onChange }) => {
  const { data: pageData, isLoading } = useQuery({
    queryKey: ['metadata-definitions', categoryId],
    queryFn: () => metadataApi.getByCategory(categoryId, 0, 100),
  });

  const definitions = pageData?.content || [];

  if (isLoading || definitions.length === 0) {
    return null;
  }

  const getValue = (def: MetadataDefinition): string => {
    const found = values.find(
      (v) =>
        (v.definitionId && v.definitionId === def.id) ||
        (v.key && def.name && v.key.toLowerCase() === def.name.toLowerCase()) ||
        (v.key && def.label && v.key.toLowerCase() === def.label.toLowerCase())
    );
    return found ? found.value : '';
  };

  const handleFieldValueChange = (def: MetadataDefinition, newValue: string) => {
    const existingIndex = values.findIndex(
      (v) =>
        (v.definitionId && v.definitionId === def.id) ||
        (v.key && def.name && v.key.toLowerCase() === def.name.toLowerCase()) ||
        (v.key && def.label && v.key.toLowerCase() === def.label.toLowerCase())
    );

    const updated = [...values];
    const newEntry: DocumentMetadataValue = {
      definitionId: def.id,
      key: def.name,
      value: newValue,
    };

    if (existingIndex >= 0) {
      updated[existingIndex] = newEntry;
    } else {
      updated.push(newEntry);
    }

    onChange(updated);
  };

  const handleMultiSelectChange = (def: MetadataDefinition, option: string, checked: boolean) => {
    const currentRaw = getValue(def);
    const selectedOptions = currentRaw
      ? currentRaw.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    let updatedOptions: string[];
    if (checked) {
      updatedOptions = [...selectedOptions, option];
    } else {
      updatedOptions = selectedOptions.filter((o) => o !== option);
    }

    handleFieldValueChange(def, updatedOptions.join(','));
  };

  const renderFieldInput = (def: MetadataDefinition) => {
    const currentValue = getValue(def);

    switch (def.type) {
      case 'STRING':
        return (
          <input
            type="text"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            placeholder={`Saisir ${def.label.toLowerCase()}...`}
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary"
          />
        );

      case 'URL':
        return (
          <input
            type="url"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            placeholder="https://..."
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary font-mono"
          />
        );

      case 'INTEGER':
        return (
          <input
            type="number"
            step="1"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            placeholder="0"
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary"
          />
        );

      case 'DECIMAL':
        return (
          <input
            type="number"
            step="any"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            placeholder="0.00"
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary focus:ring-1 focus:ring-brand-primary"
          />
        );

      case 'DATE':
        return (
          <input
            type="date"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
          />
        );

      case 'DATETIME':
        return (
          <input
            type="datetime-local"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
          />
        );

      case 'BOOLEAN':
        const isChecked = currentValue === 'true';
        return (
          <label className="inline-flex items-center gap-2 cursor-pointer pt-1">
            <input
              type="checkbox"
              checked={isChecked}
              onChange={(e) => handleFieldValueChange(def, e.target.checked ? 'true' : 'false')}
              className="w-4 h-4 text-brand-primary border-brand-border rounded focus:ring-brand-primary bg-brand-surface"
            />
            <span className="text-xs text-brand-text font-medium">{def.label}</span>
          </label>
        );

      case 'SELECT':
        return (
          <select
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            required={def.required}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
          >
            <option value="">-- Choisir une option --</option>
            {(def.options || []).map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </select>
        );

      case 'MULTI_SELECT':
        const selectedList = currentValue
          ? currentValue.split(',').map((s) => s.trim()).filter(Boolean)
          : [];
        return (
          <div className="p-2.5 bg-brand-surface border border-brand-border rounded-lg flex flex-wrap gap-3">
            {(def.options || []).map((opt) => {
              const optChecked = selectedList.includes(opt);
              return (
                <label key={opt} className="inline-flex items-center gap-1.5 cursor-pointer text-xs text-brand-text">
                  <input
                    type="checkbox"
                    checked={optChecked}
                    onChange={(e) => handleMultiSelectChange(def, opt, e.target.checked)}
                    className="w-3.5 h-3.5 text-brand-primary border-brand-border rounded focus:ring-brand-primary bg-brand-surface"
                  />
                  <span>{opt}</span>
                </label>
              );
            })}
          </div>
        );

      default:
        return (
          <input
            type="text"
            value={currentValue}
            onChange={(e) => handleFieldValueChange(def, e.target.value)}
            className="w-full bg-brand-surface border border-brand-border rounded-lg px-3 py-2 text-xs text-brand-text focus:outline-none focus:border-brand-primary"
          />
        );
    }
  };

  return (
    <div className="border border-brand-border p-3.5 bg-brand-surface rounded-lg space-y-3">
      <div className="flex items-center gap-2 border-b border-brand-border pb-2">
        <Sliders className="w-4 h-4 text-brand-primary" />
        <h4 className="font-bold text-[11px] uppercase tracking-wider text-brand-muted">
          Métadonnées spécifiques
        </h4>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {definitions.map((def) => (
          <div key={def.id} className={def.type === 'MULTI_SELECT' ? 'col-span-full' : ''}>
            {def.type !== 'BOOLEAN' && (
              <label className="block text-[11px] font-bold uppercase tracking-wider text-brand-muted mb-1">
                {def.label} {def.required && <span className="text-red-500">*</span>}
              </label>
            )}
            {renderFieldInput(def)}
          </div>
        ))}
      </div>
    </div>
  );
};
