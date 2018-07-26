-- phpMyAdmin SQL Dump
-- version 4.5.4.1deb2ubuntu2
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Jul 20, 2018 at 09:13 PM
-- Server version: 5.7.22-0ubuntu0.16.04.1
-- PHP Version: 7.0.30-0ubuntu0.16.04.1

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `ultimategdbot`
--
CREATE DATABASE IF NOT EXISTS `ultimategdbot` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `ultimategdbot`;

-- --------------------------------------------------------

--
-- Table structure for table `awarded_level`
--

CREATE TABLE `awarded_level` (
  `insert_date` datetime NOT NULL,
  `level_id` bigint(20) NOT NULL,
  `downloads` bigint(20) NOT NULL,
  `likes` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `gd_mod`
--

CREATE TABLE `gd_mod` (
  `account_id` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `elder` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `global_settings`
--

CREATE TABLE `global_settings` (
  `id` int(11) NOT NULL,
  `channel_debug_logs` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `guild_settings`
--

CREATE TABLE `guild_settings` (
  `guild_id` bigint(20) NOT NULL,
  `role_awarded_levels` bigint(20) NOT NULL DEFAULT '0',
  `channel_awarded_levels` bigint(20) NOT NULL DEFAULT '0',
  `role_gd_moderators` bigint(20) NOT NULL DEFAULT '0',
  `channel_gd_moderators` bigint(20) NOT NULL DEFAULT '0',
  `channel_timely_levels` bigint(20) NOT NULL DEFAULT '0',
  `role_timely_levels` bigint(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `timely_level`
--

CREATE TABLE `timely_level` (
  `insert_date` datetime NOT NULL,
  `level_id` bigint(20) NOT NULL,
  `weekly` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `user_settings`
--

CREATE TABLE `user_settings` (
  `user_id` bigint(20) NOT NULL DEFAULT '0',
  `gd_user_id` bigint(20) NOT NULL,
  `link_activated` tinyint(1) NOT NULL,
  `confirmation_token` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `awarded_level`
--
ALTER TABLE `awarded_level`
  ADD PRIMARY KEY (`level_id`);

--
-- Indexes for table `gd_mod`
--
ALTER TABLE `gd_mod`
  ADD PRIMARY KEY (`account_id`);

--
-- Indexes for table `global_settings`
--
ALTER TABLE `global_settings`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `guild_settings`
--
ALTER TABLE `guild_settings`
  ADD PRIMARY KEY (`guild_id`);

--
-- Indexes for table `timely_level`
--
ALTER TABLE `timely_level`
  ADD PRIMARY KEY (`level_id`);

--
-- Indexes for table `user_settings`
--
ALTER TABLE `user_settings`
  ADD PRIMARY KEY (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `global_settings`
--
ALTER TABLE `global_settings`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
