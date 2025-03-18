CREATE TABLE `db_user_users` (
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `username` varchar(40) NOT NULL,
    `email` varchar(40) DEFAULT NULL,
    `email_verified` varchar(1) DEFAULT 'n',
    `first_name` varchar(40) DEFAULT NULL,
    `last_name` varchar(40) DEFAULT NULL,
    `enabled` tinyint(1) DEFAULT 1,
    `temp_password` varchar(40) DEFAULT NULL,
    `required_actions` varchar(80) DEFAULT NULL,
    `updated` timestamp NULL DEFAULT current_timestamp(),
    `custom_attr` varchar(40) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `db_user_roles` (
    `username` varchar(40) NOT NULL,
    `name` varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;