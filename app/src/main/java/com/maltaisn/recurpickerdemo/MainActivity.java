/*
 * Copyright (c) Nicolas Maltais 2018
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.maltaisn.recurpickerdemo;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.maltaisn.recurpicker.Recurrence;
import com.maltaisn.recurpicker.RecurrenceFormat;
import com.maltaisn.recurpicker.RecurrencePickerDialog;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements RecurrencePickerDialog.RecurrenceSelectedCallback {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long DAYS_100 = DateUtils.DAY_IN_MILLIS * 100;

    private CheckBox maxFreqCheck;
    private CheckBox maxEndCountCheck;
    private EditText maxFreqValue;
    private EditText maxEndCountValue;
    private EditText defaultEndDateValue;
    private EditText defaultEndCountValue;

    private Button dialogPickerBtn;
    private TextView dialogPickerValue;
    private TextView dialogPickerNextValue;
    private ImageButton dialogPickerPreviousBtn;
    private ImageButton dialogPickerNextBtn;

    private Calendar startDate;
    private DateFormat dateFormatLong;
    private Recurrence recurrence;
    private int selectedRecur;
    private int recurCount;
    private ArrayList<Long> recurrenceList;

    private RecurrenceFormat formatter;
    private Calendar maxEndDate;

    @Override
    protected void onCreate(final Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        startDate = Calendar.getInstance();
        recurrence = new Recurrence(startDate.getTimeInMillis(), Recurrence.NONE);  // Does not repeat
        recurrenceList = new ArrayList<>();
        if (state != null) {
            recurrence = state.getParcelable("recurrence");
            startDate.setTimeInMillis(state.getLong("startDate"));
            selectedRecur = state.getInt("selectedRecur");
            recurCount = state.getInt("recurCount");
            maxEndDate = Calendar.getInstance();
            maxEndDate.setTimeInMillis(state.getLong("maxEndDate"));

            // Convert back long array to arraylist
            //noinspection ConstantConditions
            for (long r : state.getLongArray("recurrenceList")) {
                recurrenceList.add(r);
            }
        }

        // Get views
        final EditText startDateValue = findViewById(R.id.edt_start_date);
        maxFreqCheck = findViewById(R.id.chk_max_freq);
        final CheckBox maxEndDateCheck = findViewById(R.id.chk_max_end_date);
        maxEndCountCheck = findViewById(R.id.chk_max_end_count);
        maxFreqValue = findViewById(R.id.edt_max_freq);
        final EditText maxEndDateValue = findViewById(R.id.edt_max_end_date);
        maxEndCountValue = findViewById(R.id.edt_max_end_count);
        final CheckBox defaultEndDateCheck = findViewById(R.id.chk_default_end_date_use_period);
        final TextView defaultEndDateUnit = findViewById(R.id.edt_default_end_date_unit);
        defaultEndDateValue = findViewById(R.id.edt_default_end_date);
        defaultEndCountValue = findViewById(R.id.edt_default_end_count);

        final CheckBox optionListEnabledCheck = findViewById(R.id.chk_option_list_enabled);
        final CheckBox creatorEnabledCheck = findViewById(R.id.chk_creator_enabled);
        final CheckBox showHeaderCheck = findViewById(R.id.chk_show_header);
        final CheckBox showDoneBtnCheck = findViewById(R.id.chk_show_done_btn);
        final CheckBox showCancelBtnCheck = findViewById(R.id.chk_show_cancel_btn);

        dialogPickerBtn = findViewById(R.id.btn_recur_picker);
        dialogPickerValue = findViewById(R.id.txv_recurrence);
        dialogPickerNextValue = findViewById(R.id.txv_event_date);
        dialogPickerPreviousBtn = findViewById(R.id.btn_event_prev);
        dialogPickerNextBtn = findViewById(R.id.btn_event_next);

        // Set the date formats
        Locale locale = getResources().getConfiguration().locale;
        dateFormatLong = new SimpleDateFormat("EEE MMM dd, yyyy", locale);  // Sun Dec 31, 2017
        final DateFormat dateFormatShort = new SimpleDateFormat("dd-MM-yyyy", locale);  // 31-12-2017

        // Formatter for formatting the recurrences
        formatter = new RecurrenceFormat(this, dateFormatLong);

        // Start date picker edit text
        startDateValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        startDate.set(year, month, day);
                        startDateValue.setText(dateFormatLong.format(startDate.getTimeInMillis()));

                        // Also needs to update current recurrence
                        recurrence.setStartDate(startDate.getTimeInMillis());
                        selectRecurrence(recurrence);  // Update interface

                        // Check if end date is before start date
                        if (maxEndDate != null && !Recurrence.isOnSameDayOrAfter(maxEndDate, startDate)) {
                            // Change end date to 100 days after start date
                            maxEndDate.setTimeInMillis(startDate.getTimeInMillis() + DAYS_100);
                            if (maxEndDateCheck.isChecked()) {
                                maxEndDateValue.setText(dateFormatLong.format(maxEndDate.getTime()));
                            }
                        }
                    }
                }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });
        startDateValue.setText(dateFormatLong.format(startDate.getTime()));

        // Max frequency views
        maxFreqCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxFreqValue.setEnabled(isChecked);
                updateDialogPickerBtnEnabled();
            }
        });
        maxFreqCheck.setChecked(true);
        maxFreqValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int freq = Integer.valueOf(text);
                if (freq == 0) {
                    maxFreqValue.getText().replace(0, 1, "1");
                }
            }
        });
        maxFreqValue.setText(String.valueOf(99));
        maxFreqValue.setEnabled(maxFreqCheck.isChecked());

        // Max end count views
        maxEndCountCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxEndCountValue.setEnabled(isChecked);
                updateDialogPickerBtnEnabled();
            }
        });
        maxEndCountCheck.setChecked(true);
        maxEndCountValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) return;

                int count = Integer.valueOf(text);
                if (count == 0) {
                    maxFreqValue.getText().replace(0, 1, "1");
                } else {
                    // Change default end count value if larger than maximum
                    String defaultCountStr = defaultEndCountValue.getText().toString();
                    if (!defaultCountStr.isEmpty()) {
                        int defaultCount = Integer.valueOf(defaultCountStr);
                        if (defaultCount > count) {
                            defaultEndCountValue.setText(String.valueOf(count));
                        }
                    }
                }
            }
        });
        maxEndCountValue.setText(String.valueOf(999));
        maxEndCountValue.setEnabled(maxEndCountCheck.isChecked());

        // Max end date picker edit text
        maxEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                maxEndDateValue.setEnabled(isChecked);
                if (isChecked) {
                    if (maxEndDate == null) {
                        // No max end date set, make it 100 days after start date
                        maxEndDate = Calendar.getInstance();
                        maxEndDate.setTimeInMillis(startDate.getTimeInMillis() + DAYS_100);
                    }
                    maxEndDateValue.setText(dateFormatLong.format(maxEndDate.getTime()));
                } else {
                    maxEndDateValue.setText(getString(R.string.max_end_date_none));
                }
            }
        });
        maxEndDateCheck.setChecked(false);
        if (!maxEndDateCheck.isChecked()) {
            maxEndDateValue.setText(getString(R.string.max_end_date_none));
        }
        maxEndDateValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        maxEndDate.set(year, month, day);
                        maxEndDateValue.setText(dateFormatLong.format(maxEndDate.getTime()));
                    }
                }, maxEndDate.get(Calendar.YEAR), maxEndDate.get(Calendar.MONTH), maxEndDate.get(Calendar.DAY_OF_MONTH));
                dialog.getDatePicker().setMinDate(startDate.getTimeInMillis());
                dialog.show();
            }
        });
        maxEndDateValue.setEnabled(maxEndDateCheck.isChecked());
        if (maxEndDateCheck.isChecked()) {
            maxEndDate.setTimeInMillis(-1);
            maxEndCountValue.setText(dateFormatLong.format(maxEndDate.getTime()));
        }

        // Default end date views
        defaultEndDateCheck.setChecked(true);
        defaultEndDateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String text = defaultEndDateValue.getText().toString();
                int count = 1;
                if (!text.isEmpty()) {
                    count = Integer.valueOf(text);
                }
                defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                        R.string.default_end_date_periods : R.string.default_end_date_days), count));
            }
        });
        defaultEndDateValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) {
                    return;
                }

                int count = Integer.valueOf(text);
                if (count == 0) {
                    count = 1;
                    maxFreqValue.getText().replace(0, 1, "1");
                }

                defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                        R.string.default_end_date_periods : R.string.default_end_date_days), count));
            }
        });
        defaultEndDateValue.setText(String.valueOf(3));
        defaultEndDateUnit.setText(MessageFormat.format(getString(defaultEndDateCheck.isChecked() ?
                R.string.default_end_date_periods : R.string.default_end_date_days), 3));

        // Default end count view
        defaultEndCountValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                updateDialogPickerBtnEnabled();
                if (text.isEmpty()) {
                    return;
                }

                int count = Integer.valueOf(text);
                if (count == 0) {
                    defaultEndCountValue.getText().replace(0, 1, "1");
                } else {
                    // Change default end count if greater than maximum end count
                    String maxCountStr = maxEndCountValue.getText().toString();
                    if (!maxCountStr.isEmpty()) {
                        int maxCount = Integer.valueOf(maxCountStr);
                        if (count > maxCount) defaultEndCountValue.getText().replace(
                                0, text.length(), String.valueOf(maxCount));
                    }
                }
            }
        });
        defaultEndCountValue.setText(String.valueOf(5));

        // Set up checkbox options
        optionListEnabledCheck.setChecked(true);
        optionListEnabledCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked && !creatorEnabledCheck.isChecked()) {
                    // Both can't be disabled
                    creatorEnabledCheck.setChecked(true);
                }
            }
        });
        creatorEnabledCheck.setChecked(true);
        creatorEnabledCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked && !optionListEnabledCheck.isChecked()) {
                    // Both can't be disabled
                    optionListEnabledCheck.setChecked(true);
                }
            }
        });

        showHeaderCheck.setChecked(true);
        showDoneBtnCheck.setChecked(false);
        showCancelBtnCheck.setChecked(false);

        // Set up dialog recurrence picker
        final RecurrencePickerDialog pickerDialog = new RecurrencePickerDialog();
        pickerDialog.setDateFormat(dateFormatShort, dateFormatLong);
        dialogPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the settings
                pickerDialog.setMaxFrequency(maxFreqCheck.isChecked() ?
                        Integer.valueOf(maxFreqValue.getText().toString()) : -1)
                        .setMaxEndDate(maxEndDateCheck.isChecked() ? maxEndDate.getTimeInMillis() : -1)
                        .setMaxEventCount(maxEndCountCheck.isChecked() ?
                                Integer.valueOf(maxEndCountValue.getText().toString()) : -1)
                        .setDefaultEndDate(defaultEndDateCheck.isChecked(),
                                Integer.valueOf(defaultEndDateValue.getText().toString()))
                        .setDefaultEndCount(Integer.valueOf(defaultEndCountValue.getText().toString()))
                        .setEnabledModes(optionListEnabledCheck.isChecked(), creatorEnabledCheck.isChecked())
                        .setShowHeaderInOptionList(showHeaderCheck.isChecked())
                        .setShowDoneButtonInOptionList(showDoneBtnCheck.isChecked())
                        .setShowCancelButton(showCancelBtnCheck.isChecked())
                        .setRecurrence(recurrence, startDate.getTimeInMillis());

                // Not necessary, but if a cancel button is shown, often dialog isn't cancelable
                pickerDialog.setCancelable(!showCancelBtnCheck.isChecked());

                // Show the recurrence dialog
                pickerDialog.show(getSupportFragmentManager(), "recur_picker_dialog");
            }
        });

        // Set up recurrence list views
        dialogPickerValue.setText(formatter.format(recurrence));

        dialogPickerPreviousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Find previous recurrence in the list
                selectedRecur--;
                Date date = new Date(recurrenceList.get(selectedRecur));
                dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                        selectedRecur + 1, dateFormatLong.format(date)));

                setButtonEnabled(dialogPickerPreviousBtn, selectedRecur > 0);
                setButtonEnabled(dialogPickerNextBtn, true);
            }
        });

        dialogPickerNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show already computed next recurrence
                selectedRecur++;
                Date date = new Date(recurrenceList.get(selectedRecur));
                dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                        selectedRecur + 1, dateFormatLong.format(date)));

                setButtonEnabled(dialogPickerPreviousBtn, true);

                if (recurCount == selectedRecur + 1) {
                    // If this is last recurrence, disable next button
                    setButtonEnabled(dialogPickerNextBtn, false);
                } else if (recurrenceList.size() == selectedRecur + 1) {
                    // If not already done, compute next recurrence based on current one
                    List<Long> next = recurrence.findRecurrencesBasedOn(recurrenceList.get(selectedRecur),
                            selectedRecur + 1, -1, 1);
                    if (next.size() == 0) {
                        recurCount = recurrenceList.size();
                        setButtonEnabled(dialogPickerNextBtn, false);
                    } else {
                        recurrenceList.add(next.get(0));
                    }
                }
            }
        });

        // Set up interface for current recurrence
        updateUIToSelection();
    }

    private void updateDialogPickerBtnEnabled() {
        dialogPickerBtn.setEnabled(
                (!maxFreqCheck.isChecked() || !maxFreqValue.getText().toString().isEmpty()) &&
                        (!maxEndCountCheck.isChecked() || !maxEndCountValue.getText().toString().isEmpty()) &&
                        (!defaultEndDateValue.getText().toString().isEmpty()) &&
                        (!defaultEndCountValue.getText().toString().isEmpty())
        );
    }

    private void updateUIToSelection() {
        int visibility = recurrence.getPeriod() == Recurrence.NONE ? View.GONE : View.VISIBLE;
        dialogPickerPreviousBtn.setVisibility(visibility);
        dialogPickerNextBtn.setVisibility(visibility);

        setButtonEnabled(dialogPickerNextBtn, selectedRecur + 1 < recurrenceList.size());
        setButtonEnabled(dialogPickerPreviousBtn, selectedRecur > 0);
        if (recurrenceList.size() == 0) {
            dialogPickerNextValue.setText(getString(R.string.dialog_picker_value_none));
        } else {
            dialogPickerNextValue.setText(MessageFormat.format(getString(R.string.dialog_picker_value),
                    selectedRecur + 1, dateFormatLong.format(recurrenceList.get(selectedRecur))));
        }
    }

    private void selectRecurrence(Recurrence r) {
        recurrence = r;
        dialogPickerValue.setText(formatter.format(recurrence));

        // Compute first two recurrences
        List<Long> next = recurrence.findRecurrences(-1, 2);
        recurrenceList = new ArrayList<>();
        selectedRecur = 0;
        recurCount = -1;
        if (next.size() == 0) {
            // No recurrences found
            recurCount = 0;
        } else {
            // One or more found
            recurrenceList.add(next.get(0));
            if (next.size() == 1) {
                recurCount = 1;
            } else {
                // Two or more found
                recurrenceList.add(next.get(1));
            }
        }

        updateUIToSelection();
    }

    private void setButtonEnabled(ImageButton btn, boolean enabled) {
        btn.setEnabled(enabled);
        btn.setAlpha(enabled ? 1f : 0.3f);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putLong("startDate", startDate.getTimeInMillis());
        state.putParcelable("recurrence", recurrence);
        state.putInt("selectedRecur", selectedRecur);
        state.putInt("recurCount", recurCount);
        state.putLong("maxEndDate", maxEndDate == null ? -1 : maxEndDate.getTimeInMillis());

        // Save recurrence list, have to convert it to primitive array
        long[] list = new long[recurrenceList.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = recurrenceList.get(i);
        }
        state.putLongArray("recurrenceList", list);
    }

    @Override
    public void onRecurrencePickerSelected(Recurrence r) {
        selectRecurrence(r);
    }

    @Override
    public void onRecurrencePickerCancelled(Recurrence r) {
        // Nothing happens
    }
}
