import os
import pandas as pd
import joblib
# --- CHANGE: Import XGBoost ---
import xgboost as xgb
# -----------------------------
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, classification_report
import traceback

from data_loader import load_and_split_data

RECOMMENDER_MODEL_PATH = "recommender_model_xgb.joblib"
LABEL_ENCODER_PATH = "recommender_label_encoder.joblib"
FEATURES_FOR_RECOMMENDER = ['cpuUsage', 'memoryUsage', 'vCPU', 'ramGb']
TARGET_COLUMN = 'target'

def train_recommender_model():
    """
    Trains an XGBoost Classifier to predict the scaling action ('target')
    based on resource usage and configuration.
    """
    try:
        print("Starting Recommender (XGBoost) model training...")

        train_df, _ = load_and_split_data()
        print(f"Loaded {len(train_df)} records for training.")

        X = train_df[FEATURES_FOR_RECOMMENDER]
        y_raw = train_df[TARGET_COLUMN]

        initial_count = len(X)
        combined_df = pd.concat([X, y_raw], axis=1)
        combined_df_cleaned = combined_df.dropna()
        final_count = len(combined_df_cleaned)

        if final_count < initial_count:
            print(f"⚠️ Dropped {initial_count - final_count} rows with missing values.")
        if final_count < 100:
            return {"status": "error", "message": f"Not enough valid training data ({final_count} records)."}

        X_cleaned = combined_df_cleaned[FEATURES_FOR_RECOMMENDER]
        y_cleaned = combined_df_cleaned[TARGET_COLUMN]
        print(f"Using {final_count} valid records for training.")

        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y_cleaned.astype(str))
        print("Target labels encoded:", dict(zip(label_encoder.classes_, label_encoder.transform(label_encoder.classes_))))
        joblib.dump(label_encoder, LABEL_ENCODER_PATH)
        print(f"Label encoder saved to {LABEL_ENCODER_PATH}")

        # Initialize and Train the XGBoost Classifier
        print("Initializing XGBClassifier...")
        # Common XGBoost parameters:
        # n_estimators: Number of boosting rounds (trees).
        # learning_rate: Step size shrinkage to prevent overfitting.
        # max_depth: Maximum depth of a tree.
        # objective='multi:softmax': For multi-class classification.
        # num_class: Needs to be set for multi:softmax.
        # random_state: For reproducibility.
        # n_jobs=-1: Use all CPU cores.
        num_classes = len(label_encoder.classes_)
        model = xgb.XGBClassifier(
            objective='multi:softmax',
            num_class=num_classes,
            n_estimators=100,
            learning_rate=0.1,
            max_depth=5,
            random_state=42,
            n_jobs=-1,
            # Use default eval_metric for multi:softmax, which is merror
        )

        print("Fitting the XGBoost model on the prepared data...")
        model.fit(X_cleaned, y_encoded)

        # --- Step 4: Save the Trained Model ---
        print(f"Saving trained model to {RECOMMENDER_MODEL_PATH}...")
        joblib.dump(model, RECOMMENDER_MODEL_PATH)

        print(f"✅ Recommender (XGBoost) model trained on {final_count} records and saved to {RECOMMENDER_MODEL_PATH}")
        return {
            "status": "success",
            "message": f"Recommender (XGBoost) model trained on {final_count} records and saved.",
            "label_encoding": dict(zip(label_encoder.classes_, label_encoder.transform(label_encoder.classes_)))
        }

    except Exception as e:
        print(f"❌ Error during recommender (XGBoost) model training: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": str(e)}

def validate_recommender_model():
    """
    Loads the trained XGBoost model and validates it against the test set.
    Reports accuracy and a classification report.
    """
    try:
        print("Starting Recommender (XGBoost) model validation...")

        # Load Model, Encoder, and Test Data ---
        if not os.path.exists(RECOMMENDER_MODEL_PATH) or not os.path.exists(LABEL_ENCODER_PATH):
            return {
                "status": "error",
                "message": f"Model ({RECOMMENDER_MODEL_PATH}) or Label Encoder ({LABEL_ENCODER_PATH}) not found. Run training first."
            }

        # Load the saved model and encoder
        print("Loading trained model and label encoder...")
        model = joblib.load(RECOMMENDER_MODEL_PATH)
        label_encoder = joblib.load(LABEL_ENCODER_PATH)

        # Load the test data (second element)
        _, test_df = load_and_split_data()
        print(f"Loaded {len(test_df)} records for validation.")

        # Prepare Test Data ---
        X_test_raw = test_df[FEATURES_FOR_RECOMMENDER]
        y_test_raw = test_df[TARGET_COLUMN]

        # Handle potential missing values in the test set
        initial_count_test = len(X_test_raw)
        combined_test_df = pd.concat([X_test_raw, y_test_raw], axis=1)
        combined_test_df_cleaned = combined_test_df.dropna()
        final_count_test = len(combined_test_df_cleaned)

        if final_count_test < initial_count_test:
             print(f"⚠️ Using {final_count_test} valid records for validation (dropped {initial_count_test - final_count_test} rows with NaNs).")

        if final_count_test == 0:
             return {"status": "error", "message": "No valid records in test set after dropping NaNs."}

        X_test_cleaned = combined_test_df_cleaned[FEATURES_FOR_RECOMMENDER]
        y_test_cleaned_labels = combined_test_df_cleaned[TARGET_COLUMN] # Actual string labels

        # Encode the true labels from the test set for comparison
        y_test_encoded = label_encoder.transform(y_test_cleaned_labels.astype(str))

        # Make Predictions ---
        print("Predicting scaling actions on the test set...")
        predictions_encoded = model.predict(X_test_cleaned)

        #Evaluate Performance ---
        accuracy = accuracy_score(y_test_encoded, predictions_encoded)
        # Generate repory
        report_dict = classification_report(y_test_encoded, predictions_encoded,
                                            target_names=label_encoder.classes_, # Use original class names
                                            output_dict=True, zero_division=0)

        print(f"✅ Validation complete. Accuracy: {accuracy:.4f}")
    
        return {
            "status": "success",
            "message": "Recommender (XGBoost) model validated successfully.",
            "accuracy": f"{accuracy:.4f}", # Accuracy score
            "classification_report": report_dict, # Detailed metrics per class
            "info": f"Model validated on {final_count_test} records using features: {FEATURES_FOR_RECOMMENDER}"
        }

    except Exception as e:
        print(f"❌ Error during recommender model validation: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": str(e)}

