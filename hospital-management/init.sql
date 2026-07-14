CREATE DATABASE IF NOT EXISTS patient_db;
CREATE DATABASE IF NOT EXISTS doctor_db;
CREATE DATABASE IF NOT EXISTS appointment_db;
CREATE DATABASE IF NOT EXISTS billing_db;
CREATE DATABASE IF NOT EXISTS notification_db;

GRANT ALL PRIVILEGES ON patient_db.* TO 'hospital_user'@'%';
GRANT ALL PRIVILEGES ON doctor_db.* TO 'hospital_user'@'%';
GRANT ALL PRIVILEGES ON appointment_db.* TO 'hospital_user'@'%';
GRANT ALL PRIVILEGES ON billing_db.* TO 'hospital_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'hospital_user'@'%';
FLUSH PRIVILEGES;
