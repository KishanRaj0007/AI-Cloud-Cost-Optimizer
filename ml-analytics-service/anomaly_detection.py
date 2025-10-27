import os
import pandas as pd
import numpy as np
import joblib
from sklearn.ensemble import IsolationForest
import traceback

from data_loader import load_and_split_data

ANOMALY_MODEL_PATH = "anomaly_model.joblib"
FEATURES_FOR_ANOMALY = ['cpuUsage', 'memoryUsage'] # Correct camelCase names from Java model

def train_anomaly_model():
    """
    Trains an Isolation Forest model on historical performance data (CPU, Memory).
    """
    try:
        print("Starting Isolation Forest model training...")

        train_df, _ = load_and_split_data()

        print(f"Loaded {len(train_df)} records for training.")

        features_df = train_df[FEATURES_FOR_ANOMALY]

        initial_count = len(features_df)
        features_df = features_df.dropna()
        final_count = len(features_df)

        if final_count < initial_count:
            print(f"⚠️ Dropped {initial_count - final_count} rows with missing CPU/Memory values.")

        if final_count < 50:
             return {
                 "status": "error",
                 "message": f"Not enough valid training data after dropping NaNs ({final_count} records). Need at least 50."
             }

        print(f"Using {final_count} valid records for training.")

        contamination_level = 0.05
        print(f"Initializing IsolationForest model (contamination={contamination_level})...")
        model = IsolationForest(
            n_estimators=100,
            contamination=contamination_level,
            random_state=42,
            n_jobs=-1
        )

        print("Fitting the model on the prepared feature data...")
        model.fit(features_df)

        print(f"Saving trained model to {ANOMALY_MODEL_PATH}...")
        joblib.dump(model, ANOMALY_MODEL_PATH)

        print(f"✅ Anomaly detection model trained on {final_count} records and saved to {ANOMALY_MODEL_PATH}")

        return {
            "status": "success",
            "message": f"Anomaly detection model trained on {final_count} records and saved to {ANOMALY_MODEL_PATH}"
        }

    except Exception as e:
        print(f"❌ Error during anomaly model training: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": str(e)}

def validate_anomaly_model():
    """
    Loads the trained Isolation Forest model, predicts anomalies on the test set,
    and returns the details of the detected anomalies.
    """
    try:
        print("Starting Isolation Forest model validation...")

        if not os.path.exists(ANOMALY_MODEL_PATH):
            return {"status": "error", "message": f"Model not found at {ANOMALY_MODEL_PATH}."}

        model = joblib.load(ANOMALY_MODEL_PATH)
        _, test_df = load_and_split_data() 
        print(f"Loaded {len(test_df)} records for validation.")

        features_df_test = test_df[FEATURES_FOR_ANOMALY]
        initial_count_test = len(features_df_test)

        nan_rows_mask = features_df_test.isna().any(axis=1)
        valid_rows_mask = ~nan_rows_mask

        features_df_test_valid = features_df_test[valid_rows_mask]
        final_count_test = len(features_df_test_valid)

        if final_count_test < initial_count_test:
            print(f"⚠️ Using {final_count_test} valid records for prediction (ignoring {initial_count_test - final_count_test} rows with NaNs).")

        if final_count_test == 0:
             return {"status": "error", "message": "No valid records for prediction after dropping NaNs."}

        print("Predicting anomalies on the valid test set records...")
        predictions_on_valid = model.predict(features_df_test_valid)

        valid_indices = test_df.index[valid_rows_mask]

        anomaly_indices_in_valid_subset = np.where(predictions_on_valid == -1)[0]

        original_anomaly_indices = valid_indices[anomaly_indices_in_valid_subset]

        columns_to_show = ['timestamp', 'cpuUsage', 'memoryUsage']
        anomalous_rows_df_filtered = test_df.loc[original_anomaly_indices, columns_to_show]

        anomalous_rows_list = anomalous_rows_df_filtered.to_dict('records')

        for row in anomalous_rows_list:
             if isinstance(row.get('timestamp'), pd.Timestamp):
                  row['timestamp'] = row['timestamp'].isoformat()
        # --------------------------------------------------------

        num_anomalies = len(anomalous_rows_list)
        total_predictions = final_count_test # Total valid records predicted on
        anomaly_percentage = (num_anomalies / total_predictions) * 100 if total_predictions > 0 else 0

        print(f"✅ Validation complete. Found {num_anomalies} anomalies ({anomaly_percentage:.2f}%).")

        return {
            "status": "success",
            "message": "Anomaly detection model validated successfully.",
            "total_test_records_valid": total_predictions,
            "anomalies_detected": num_anomalies,
            "anomaly_percentage": f"{anomaly_percentage:.2f}%",
            "info": f"Model predicted anomalies based on features: {FEATURES_FOR_ANOMALY}",
            "anomalies": anomalous_rows_list
        }

    except Exception as e:
        print(f"❌ Error during anomaly model validation: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": str(e)}

