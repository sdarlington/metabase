import React, { useCallback, useRef } from "react";
import { t } from "ttag";
import _ from "underscore";

import { isVirtualCardId } from "metabase/lib/saved-questions";
import { SchemaTableAndFieldDataSelector } from "metabase/query_builder/components/DataSelector";

import Field from "metabase-lib/lib/metadata/Field";
import Fields from "metabase/entities/fields";

import { StyledSelectButton } from "./MappedFieldPicker.styled";

type CollapsedPickerProps = {
  isTriggeredComponentOpen: boolean;
  open: () => void;
  close: () => void;
};

type MappedFieldPickerOwnProps = {
  field: {
    value: number | null;
    onChange: (fieldId: number) => void;
  };
  formField: {
    databaseId: number;
  };
  fieldObject?: Field;
  tabIndex?: number;
};

type MappedFieldPickerStateProps = {
  fieldObject?: Field;
};

type MappedFieldPickerProps = MappedFieldPickerOwnProps &
  MappedFieldPickerStateProps;

const query = {
  id: (state: unknown, { field }: MappedFieldPickerOwnProps) =>
    field.value || null,

  // When using Field object loader, it passes the field object as a `field` prop
  // and overwrites form's `field` prop. Entity alias makes it pass the `fieldObject` prop instead
  entityAlias: "fieldObject",

  loadingAndErrorWrapper: false,
};

function MappedFieldPicker({
  field,
  formField,
  fieldObject,
  tabIndex,
}: MappedFieldPickerProps) {
  const { value: selectedFieldId = null, onChange } = field;
  const { databaseId = null } = formField;

  const selectButtonRef = useRef<HTMLDivElement>();

  const focusSelectButton = useCallback(() => {
    selectButtonRef.current?.focus();
  }, []);

  const onFieldChange = useCallback(
    fieldId => {
      onChange(fieldId);
      selectButtonRef.current?.focus();
    },
    [onChange],
  );

  const renderTriggerElement = useCallback(
    ({ open }: CollapsedPickerProps) => {
      const label = fieldObject
        ? fieldObject.displayName({ includeTable: true })
        : t`None`;
      return (
        <StyledSelectButton
          hasValue={!!fieldObject}
          tabIndex={tabIndex}
          onKeyUp={e => {
            if (e.key === "Enter") {
              open();
            }
          }}
          ref={selectButtonRef}
        >
          {label}
        </StyledSelectButton>
      );
    },
    [fieldObject, tabIndex],
  );

  // TODO explain the need to skip virtual card id
  const selectedTableId =
    !fieldObject || isVirtualCardId(fieldObject.table.id)
      ? null
      : fieldObject?.table.id;

  return (
    <SchemaTableAndFieldDataSelector
      className="flex flex-full justify-center align-center"
      selectedDatabaseId={databaseId}
      selectedTableId={selectedTableId}
      selectedSchemaId={fieldObject?.table.schema?.id}
      selectedFieldId={selectedFieldId}
      getTriggerElementContent={renderTriggerElement}
      hasTriggerExpandControl={false}
      triggerTabIndex={tabIndex}
      setFieldFn={onFieldChange}
      onClose={focusSelectButton}
    />
  );
}
export default Fields.load(query)(MappedFieldPicker);
