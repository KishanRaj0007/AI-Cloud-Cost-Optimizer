import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
from tensorflow.keras.models import Sequential, load_model
from tensorflow.keras.layers import LSTM, Dense
from sklearn.metrics import mean_absolute_percentage_error
import os
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.layers import LSTM, Dense, BatchNormalization
from tensorflow.keras.callbacks import EarlyStopping
from data_loader import load_and_split_data # Import our data loader
from sklearn.metrics import mean_absolute_error, mean_squared_error
import traceback
from sklearn.preprocessing import StandardScaler


# --- Constants ---
MODEL_PATH = "forecasting_model_multi_config.keras"
SEQUENCE_LENGTH = 24
# --- UPDATED FEATURES ---
# Use the correct field names from the Java model / MongoDB
FEATURES = ['cost', 'ramGb', 'pricePerHour']
# -------------------------
TARGET_FEATURE_INDEX = FEATURES.index('cost')

def _preprocess_data_multi(df):
    """Prepares the data for the Multivariate LSTM model using selected features."""
    # 1. Select only the target features
    data = df[FEATURES].values

    # 2. Scale the data
    scaler = MinMaxScaler(feature_range=(0, 1))
    scaled_data = scaler.fit_transform(data)

    # 3. Create sequences
    X, y = [], []
    for i in range(SEQUENCE_LENGTH, len(scaled_data)):
        X.append(scaled_data[i-SEQUENCE_LENGTH:i])    
        y.append(scaled_data[i, TARGET_FEATURE_INDEX]) # The next 'cost'

    # Reshape X to be [samples, time steps, num_features]
    X = np.array(X)
    return X, np.array(y), scaler

def _build_model_multi(input_shape):
    """
    Builds the Keras LSTM model architecture for multivariate input,
    adding BatchNormalization layers and a slightly lower learning rate.
    """
    model = Sequential()

    # --- Layer 1 ---
    model.add(LSTM(units=50, return_sequences=True, input_shape=input_shape))
    model.add(BatchNormalization()) # Normalize outputs of the first LSTM

    # --- Layer 2 ---
    model.add(LSTM(units=50))
    model.add(BatchNormalization()) # Normalize outputs of the second LSTM

    # --- Dense Layers ---
    model.add(Dense(units=25, activation='relu')) # Explicitly add activation
    model.add(Dense(units=1)) # Final output layer

    # --- Optimizer ---
    # Further reduced learning rate
    optimizer = Adam(learning_rate=0.0005, clipnorm=1.0)
    # ------------------

    model.compile(optimizer=optimizer, loss='mean_squared_error')
    print(" Model compiled with BatchNormalization and Adam optimizer (lr=0.0005, clipnorm=1.0)")
    return model

def train_forecasting_model():
    """
    Main function to load data, train the config-multivariate model, and save it.
    """
    try:
        train_df, _ = load_and_split_data()

        print("Starting Config-Multivariate LSTM model training...")
        X_train, y_train, _ = _preprocess_data_multi(train_df)

        # input_shape = (sequence_length, num_features)
        input_shape = (X_train.shape[1], X_train.shape[2])

        model = _build_model_multi(input_shape)
        
        # Train the model
        model.fit(X_train, y_train, epochs=100, batch_size=32, verbose=1)

        # Save the trained model
        model.save(MODEL_PATH)

        print(f" Config-Multivariate LSTM forecasting model trained and saved to {MODEL_PATH}")
        return {"status": "success", "message": f"Model trained and saved to {MODEL_PATH}"}

    except Exception as e:
        print(f" Error during training: {str(e)}")
        return {"status": "error", "message": str(e)}

from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error
import numpy as np
import os
from tensorflow.keras.models import load_model
import traceback

def validate_forecasting_model():
    """
    Validates the multivariate LSTM model focusing on MAE and RMSE.
    """
    try:
        if not os.path.exists(MODEL_PATH):
            return {"status": "error", "message": f"Model not found at {MODEL_PATH}. Run training first."}

        train_df, test_df = load_and_split_data()
        print("Starting Multivariate LSTM validation (MAE + RMSE)...")

        # --- Scale data (MinMaxScaler used if trained with it) ---
        scaler = MinMaxScaler(feature_range=(0, 1))
        scaler.fit(train_df[FEATURES])
        scaled_test = scaler.transform(test_df[FEATURES])

        # --- Create sequences ---
        X_test, y_test_scaled = [], []
        for i in range(SEQUENCE_LENGTH, len(scaled_test)):
            X_test.append(scaled_test[i-SEQUENCE_LENGTH:i])        # Keep all features
            y_test_scaled.append(scaled_test[i, TARGET_FEATURE_INDEX])  # cost only

        X_test = np.array(X_test)
        y_test_scaled = np.array(y_test_scaled)

        # --- Load model & predict ---
        model = load_model(MODEL_PATH)
        predictions_scaled = model.predict(X_test)

        # --- Inverse transform predictions & actuals ---
        # Use dummy array to inverse transform
        dummy_pred = np.zeros((len(predictions_scaled), len(FEATURES)))
        dummy_pred[:, TARGET_FEATURE_INDEX] = predictions_scaled.ravel()
        predictions_real = scaler.inverse_transform(dummy_pred)[:, TARGET_FEATURE_INDEX]

        dummy_y = np.zeros((len(y_test_scaled), len(FEATURES)))
        dummy_y[:, TARGET_FEATURE_INDEX] = y_test_scaled.ravel()
        y_test_real = scaler.inverse_transform(dummy_y)[:, TARGET_FEATURE_INDEX]

        # --- Filter invalid data ---
        valid_mask = (~np.isnan(predictions_real)) & (~np.isnan(y_test_real))
        y_test_real = y_test_real[valid_mask]
        predictions_real = predictions_real[valid_mask]

        # --- Metrics ---
        if len(y_test_real) == 0:
            mae = 0.0
            rmse = 0.0
            print("No valid data points left. Metrics set to 0.")
        else:
            mae = mean_absolute_error(y_test_real, predictions_real)
            rmse = np.sqrt(mean_squared_error(y_test_real, predictions_real))
            print(f"Validation complete. MAE: {mae:.4f} | RMSE: {rmse:.4f}")

        return {
            "status": "success",
            "message": "Multivariate model validated successfully.",
            "mae": round(mae, 4),
            "rmse": round(rmse, 4),
            "info": f"Metrics use past {FEATURES} features."
        }

    except Exception as e:
        print(f"Error during validation: {str(e)}")
        traceback.print_exc()
        return {"status": "error", "message": str(e)}
